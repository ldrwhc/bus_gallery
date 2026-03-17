import re
from pathlib import Path

ROOT = Path('backend/src/main/java')
MODIFIERS = {
    'public', 'protected', 'private', 'static', 'final', 'abstract', 'synchronized',
    'default', 'native', 'strictfp', 'sealed', 'non-sealed'
}
CONTROL_KEYWORDS = {'if', 'for', 'while', 'switch', 'catch', 'return', 'case', 'default', 'try', 'do', 'new', 'throw'}
CLASS_KINDS = {'class': '类', 'interface': '接口', 'enum': '枚举', 'record': '记录'}
CLASS_REGEX = re.compile(r'\b(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)')


def parse_package(lines):
    for line in lines:
        stripped = line.strip()
        if stripped.startswith('package '):
            return stripped[len('package '):].rstrip(';')
    return 'default'


def has_javadoc(lines, start_index):
    idx = start_index - 1
    while idx >= 0 and lines[idx].strip() == '':
        idx -= 1
    if idx >= 0 and lines[idx].strip().startswith('/**'):
        return True
    return False


def collect_signature(lines, decl_index):
    signature_parts = []
    paren_depth = 0
    idx = decl_index
    length = len(lines)
    while idx < length:
        current = lines[idx].strip()
        signature_parts.append(current)
        paren_depth += current.count('(') - current.count(')')
        if paren_depth <= 0 and ('{' in current or current.endswith(';')):
            break
        if any(keyword in current for keyword in ('class ', 'interface ', 'enum ', 'record ')) and '{' in current:
            break
        idx += 1
    return ' '.join(signature_parts)


def build_class_comment(indent, class_name, kind_word, package_name):
    desc = f"{class_name}{kind_word}用于封装{class_name}相关的领域职责（所在包：{package_name}）。"
    return [f"{indent}/**", f"{indent} * {desc}", f"{indent} */"]


def build_method_comment(indent, method_name, params, return_type, is_constructor):
    action = f"{method_name}方法用于处理{method_name}相关的业务逻辑。"
    if is_constructor:
        action = f"{method_name}构造器用于初始化对象状态。"
    lines = [f"{indent}/**", f"{indent} * {action}"]
    for param in params:
        lines.append(f"{indent} * @param {param} {param}参数，详见调用方上下文。")
    if is_constructor:
        lines.append(f"{indent} * @return 构造器无返回值。")
    else:
        if return_type.lower() == 'void':
            lines.append(f"{indent} * @return 无返回值。")
        else:
            lines.append(f"{indent} * @return 返回{return_type}类型结果。")
    lines.append(f"{indent} */")
    return lines


def parse_params(param_text):
    param_text = param_text.strip()
    if not param_text:
        return []
    parts = []
    depth = 0
    current = []
    for char in param_text:
        if char == ',' and depth == 0:
            part = ''.join(current).strip()
            if part:
                parts.append(part)
            current = []
            continue
        current.append(char)
        if char in '<([{':
            depth += 1
        elif char in '>)]}':
            depth = max(0, depth - 1)
    last = ''.join(current).strip()
    if last:
        parts.append(last)
    names = []
    for part in parts:
        tokens = [t for t in part.split() if not t.startswith('@')]
        if not tokens:
            continue
        name = tokens[-1].replace('...', '')
        name = name.rstrip(')')
        names.append(name)
    return names


def extract_method_info(signature, current_class):
    if '(' not in signature or any(keyword in signature for keyword in (' class ', ' interface ', ' enum ', ' record ')):
        return None
    before_paren, after = signature.split('(', 1)
    stripped_before = before_paren.strip()
    if '.' in before_paren or stripped_before.endswith('=') or '=' in stripped_before or stripped_before.startswith('new '):
        return None
    if ' new ' in stripped_before or ' throw ' in stripped_before:
        return None
    head_tokens = stripped_before.split()
    if not head_tokens:
        return None
    first_token = head_tokens[0]
    if first_token in CONTROL_KEYWORDS:
        return None
    method_name = head_tokens[-1]
    if method_name in CONTROL_KEYWORDS:
        return None
    if len(head_tokens) == 1 and method_name != current_class:
        return None
    tokens = [t for t in head_tokens[:-1] if t not in MODIFIERS]
    tokens = [t for t in tokens if not (t.startswith('<') and t.endswith('>'))]
    if not tokens and method_name != current_class:
        return None
    return_type = current_class if method_name == current_class else (tokens[-1] if tokens else 'void')
    is_constructor = method_name == current_class
    params_section = after.rsplit(')', 1)[0]
    params = parse_params(params_section)
    return method_name, params, return_type, is_constructor


def process_file(path: Path):
    lines = path.read_text(encoding='utf-8').split('\n')
    package_name = parse_package(lines)
    new_lines = []
    brace_depth = 0
    class_stack = []
    pending_classes = []
    skip_until = -1
    length = len(lines)
    for i in range(length):
        line = lines[i]
        stripped = line.strip()
        inserted_comment = False

        if i > skip_until and stripped:
            trigger = False
            if stripped.startswith('@'):
                trigger = True
            elif any(keyword in stripped for keyword in ('class ', 'interface ', 'enum ', 'record ')):
                trigger = True
            elif '(' in stripped:
                trigger = True
            if trigger:
                decl_idx = i
                while decl_idx < length and lines[decl_idx].strip().startswith('@'):
                    decl_idx += 1
                signature = collect_signature(lines, decl_idx) if decl_idx < length else ''
                if signature:
                    class_match = CLASS_REGEX.search(signature)
                    if class_match and not has_javadoc(lines, i):
                        indent = re.match(r'\s*', lines[i]).group(0)
                        kind = class_match.group(1)
                        class_name = class_match.group(2)
                        new_lines.extend(build_class_comment(indent, class_name, CLASS_KINDS.get(kind, '类'), package_name))
                        pending_classes.append((class_name, kind))
                        skip_until = decl_idx
                        inserted_comment = True
                    else:
                        current_class = class_stack[-1][0] if class_stack else None
                        if current_class and not has_javadoc(lines, i):
                            method_info = extract_method_info(signature, current_class)
                            if method_info:
                                method_name, params, return_type, is_constructor = method_info
                                indent = re.match(r'\s*', lines[i]).group(0)
                                new_lines.extend(build_method_comment(indent, method_name, params, return_type, is_constructor))
                                skip_until = decl_idx
                                inserted_comment = True

        new_lines.append(line)

        brace_depth += line.count('{') - line.count('}')
        while class_stack and brace_depth < class_stack[-1][1]:
            class_stack.pop()
        if '{' in line and pending_classes:
            class_name, kind = pending_classes.pop(0)
            class_stack.append((class_name, brace_depth, kind))

    path.write_text('\n'.join(new_lines), encoding='utf-8')


def main():
    for java_file in ROOT.rglob('*.java'):
        process_file(java_file)


if __name__ == '__main__':
    main()
