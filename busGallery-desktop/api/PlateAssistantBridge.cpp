#include "PlateAssistantBridge.h"

#include <QCoreApplication>
#include <QDir>
#include <QFileInfo>
#include <QJsonDocument>
#include <QJsonObject>
#include <QProcess>

static QString findScriptPath() {
    const QString appDir = QCoreApplication::applicationDirPath();
    const QStringList candidates = {
        QDir(appDir).filePath("plate_assistant.py"),
        QDir(appDir).filePath("../plate_assistant.py"),
        QDir(appDir).filePath("../../plate_assistant.py"),
    };
    for (const QString& c : candidates) {
        if (QFileInfo(c).exists())
            return QFileInfo(c).absoluteFilePath();
    }
    return {};
}

static QJsonObject parseJson(const QString& text) {
    const QString trimmed = text.trimmed();
    QJsonParseError e;
    QJsonDocument doc = QJsonDocument::fromJson(trimmed.toUtf8(), &e);
    if (e.error == QJsonParseError::NoError && doc.isObject())
        return doc.object();
    // Some runtimes print logs around JSON — extract widest {...} block
    int begin = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');
    if (begin >= 0 && end > begin) {
        doc = QJsonDocument::fromJson(trimmed.mid(begin, end - begin + 1).toUtf8(), &e);
        if (e.error == QJsonParseError::NoError && doc.isObject())
            return doc.object();
    }
    return {};
}

PlateRecognizeResult PlateAssistantBridge::recognizeFromImage(const QString& imagePath, int timeoutMs) {
    PlateRecognizeResult out;
    if (imagePath.trimmed().isEmpty()) {
        out.error = QStringLiteral("未提供图片路径");
        return out;
    }

    const QString script = findScriptPath();
    if (script.isEmpty()) {
        out.error = QStringLiteral("未找到 plate_assistant.py");
        return out;
    }

    QProcess proc;
    QProcessEnvironment env = QProcessEnvironment::systemEnvironment();
    env.insert("PYTHONUTF8", "1");
    env.insert("PYTHONIOENCODING", "utf-8");
    proc.setProcessEnvironment(env);

    // Try python, then py -3
    QStringList args;
    args << script << "recognize" << "--image" << imagePath;

    QString program = "python";
    proc.setProgram(program);
    proc.setArguments(args);
    proc.start();
    if (!proc.waitForStarted(5000)) {
        // fallback: py -3
        program = "py";
        QStringList args2;
        args2 << "-3" << script << "recognize" << "--image" << imagePath;
        proc.setProgram(program);
        proc.setArguments(args2);
        proc.start();
        if (!proc.waitForStarted(5000)) {
            out.error = QStringLiteral("无法启动 Python 进程");
            return out;
        }
    }

    if (!proc.waitForFinished(timeoutMs > 0 ? timeoutMs : 45000)) {
        proc.kill();
        proc.waitForFinished(2000);
        out.error = QStringLiteral("识别超时");
        return out;
    }

    QString raw = QString::fromUtf8(proc.readAllStandardOutput()).trimmed();
    if (raw.isEmpty() && proc.exitCode() != 0) {
        out.error = QString::fromLocal8Bit(proc.readAllStandardError()).trimmed();
        if (out.error.isEmpty()) out.error = QStringLiteral("识别进程失败");
        return out;
    }

    QJsonObject obj = parseJson(raw);
    if (obj.isEmpty()) {
        out.error = QStringLiteral("识别结果非有效JSON");
        return out;
    }

    out.ok = obj.value("ok").toBool(false);
    out.plate = obj.value("plate").toString().trimmed();
    out.confidence = obj.value("confidence").toDouble(0.0);
    out.source = obj.value("source").toString().trimmed();
    out.error = obj.value("error").toString().trimmed();
    if (!out.ok && out.error.isEmpty()) out.error = QStringLiteral("识别失败");
    return out;
}
