import os, glob
files = glob.glob(r'D:\code\bus-gallery\frontend\dist\assets\Stats-*.js')
if files:
    f = files[0]
    c = open(f, encoding='utf-8').read()
    print(f'Chunk: {os.path.basename(f)} ({len(c)} bytes)')
    for tag in ['_tipLines', 'filter(Boolean', 'rank-medal', '#f5f7fb', 'min-width:2px']:
        print(f'  {tag}: {tag in c}')
else:
    print('No Stats chunk found')
