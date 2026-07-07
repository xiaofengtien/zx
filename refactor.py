import os

def replace_in_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except:
        return False
        
    new_content = content.replace('com.ruoyi', 'com.zx')
    new_content = new_content.replace('ruoyi', 'zx')
    new_content = new_content.replace('RuoYi', 'Zx')
    new_content = new_content.replace('RUOYI', 'ZX')
    new_content = new_content.replace('若依', '考试系统')
    
    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

def rename_dirs_and_files(path):
    for root, dirs, files in os.walk(path, topdown=False):
        for name in files:
            if 'ruoyi' in name.lower() or 'RuoYi' in name:
                new_name = name.replace('ruoyi', 'zx').replace('RuoYi', 'Zx')
                os.rename(os.path.join(root, name), os.path.join(root, new_name))
        
        for name in dirs:
            if name == '.git' or name == 'node_modules':
                continue
            if 'ruoyi' in name.lower() or 'RuoYi' in name:
                new_name = name.replace('ruoyi', 'zx').replace('RuoYi', 'Zx')
                os.rename(os.path.join(root, name), os.path.join(root, new_name))

if __name__ == '__main__':
    root_dir = '.'
    for root, dirs, files in os.walk(root_dir):
        if '.git' in root or 'node_modules' in root:
            continue
        for file in files:
            file_path = os.path.join(root, file)
            if not file_path.endswith('.py') and not file_path.endswith('.jar') and not file_path.endswith('.class'):
                replace_in_file(file_path)
                
    rename_dirs_and_files(root_dir)
