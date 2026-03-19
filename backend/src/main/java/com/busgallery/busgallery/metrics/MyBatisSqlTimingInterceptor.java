package com.busgallery.busgallery.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class MyBatisSqlTimingInterceptor implements Interceptor {

    private final DbQueryMetrics dbQueryMetrics;

    @Value("${busgallery.metrics.slow-query-ms:200}")
    private long slowQueryMs;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            Object[] args = invocation.getArgs();
            if (args == null || args.length == 0 || !(args[0] instanceof MappedStatement ms)) {
                return null;
            }
            Object param = args.length > 1 ? args[1] : null;
            BoundSql boundSql = ms.getBoundSql(param);
            String sql = boundSql == null ? "" : normalizeSql(boundSql.getSql());
            boolean write = ms.getSqlCommandType() != SqlCommandType.SELECT;
            boolean slow = elapsedMs >= slowQueryMs;
            dbQueryMetrics.record(elapsedMs, ms.getId(), sql, write, slow);
            if (slow) {
                log.warn("Slow SQL {} ms | {} | {}", elapsedMs, ms.getId(), sql);
            }
        }
    }

    private String normalizeSql(String sql) {
        if (sql == null) return "";
        String compact = sql.replaceAll("\\s+", " ").trim();
        if (compact.length() > 400) {
            return compact.substring(0, 400) + "...";
        }
        return compact;
    }
}
