import fitz,json,sys,unicodedata,re
pdf,toc,out=sys.argv[1:4]
d=fitz.open(pdf); ents=json.load(open(toc))
norm=lambda s: re.sub(r'\s+',' ',unicodedata.normalize('NFC',s)).strip().lower()
texts=[norm(d[i].get_text()) for i in range(len(d))]
res=[]; start=3; prev=None
for e in ents:
    key=norm(re.sub(r'^[\d.]+\s+','',e))
    if key=='mục lục': res.append(2); continue
    found=''
    if key==prev: start+=1
    prev=key
    for i in range(start,len(d)):
        if key in texts[i]: found=i+1; start=i; break
    res.append(found)
json.dump(res,open(out,'w')); print(res)
