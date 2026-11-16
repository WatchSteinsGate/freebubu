from pathlib import Path
from playwright.sync_api import sync_playwright,expect
from PIL import Image
import json,sys,re,io,base64
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/test-output';OUT.mkdir(parents=True,exist_ok=True);FIX=ROOT/'tests/fixtures';live='--live' in sys.argv;state={};errors=[];tokens=[]
with sync_playwright() as p:
 b=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--enable-unsafe-swiftshader']);page=b.new_page(viewport={'width':1440,'height':1100},accept_downloads=True);page.on('pageerror',lambda e:errors.append(str(e)))
 def request(r):
  if r.url.endswith('/api/state') and r.headers.get('x-cosmetics-token'):tokens.append(r.headers['x-cosmetics-token'])
 page.on('request',request)
 if not live:
  def route(r):
   if r.request.url.endswith('/api/state'):
    if r.request.method=='POST':state.clear();state.update(json.loads(r.request.post_data))
    r.fulfill(json={'name':'Tester','state':state})
   else:r.fulfill(content_type='text/html',body=(ROOT/'editor/editor.html').read_text().replace('__TOKEN__','test-token'),headers={'Content-Security-Policy':"default-src 'self'; script-src 'nonce-test-token'; worker-src blob:; style-src 'unsafe-inline'; font-src 'self' data:; img-src 'self' data: blob:; connect-src 'self' data: blob:; object-src 'none'"})
  page.route('http://editor.test/**',route)
 base='http://127.0.0.1:6969' if live else 'http://editor.test';page.goto(base+'/?type=cape');expect(page.get_by_role('button',name='Custom image',exact=True)).to_be_visible(timeout=180000)
 # Tests use a dedicated instance; reset its editor project and any saved cosmetics.
 page.get_by_role('button',name='Reset cosmetics',exact=True).click();expect(page.get_by_role('button',name='Custom image',exact=True)).to_be_visible(timeout=180000)
 page.get_by_role('button',name='Custom image',exact=True).click()
 with page.expect_file_chooser() as fc:page.get_by_role('button',name='Choose image for outside',exact=False).click()
 fc.value.set_files(FIX/'15-frames.gif');expect(page.get_by_text('15-frames.gif',exact=True)).to_be_visible(timeout=120000)
 for label in ['Full','Half','Quarter','Eighth']:expect(page.get_by_role('button',name=re.compile('^'+label+r'\s'))).to_be_visible()
 expect(page.get_by_text('4096×2048',exact=True)).to_be_visible();expect(page.get_by_text('1024×512',exact=True)).to_be_visible()
 page.screenshot(path=str(OUT/'mirror-rendering.png'),full_page=True);expect(page.get_by_role('button',name='Save cape',exact=True)).to_be_enabled(timeout=180000)
 with page.expect_download(timeout=120000) as d:page.get_by_role('button',name='Download GIF',exact=True).click()
 export=OUT/'mirror-15-frames.gif';d.value.save_as(export);gif=Image.open(export);assert gif.n_frames==15 and gif.size==(1024,512)
 for i in range(15):gif.seek(i);assert gif.info['duration']==100
 page.get_by_role('button',name='Save cape',exact=True).click();expect(page.locator('#host-status')).to_have_text('Saved.',timeout=120000)
 saved=page.request.get(base+'/api/state',headers={'X-Cosmetics-Token':tokens[-1]}).json()['state'] if live else state
 assert len(saved['cape']['frames'])==15;assert Image.open(io.BytesIO(base64.b64decode(saved['cape']['frames'][0]['png']))).size==(1024,512)
 page.screenshot(path=str(OUT/'mirror-15-frames.png'),full_page=True)
 page.reload();expect(page.get_by_role('button',name='Save cape',exact=True)).to_be_enabled(timeout=180000);expect(page.get_by_text('Saved',exact=True)).to_be_visible();assert not errors,errors
 print('PASS: original mirror worker rendering, 15-frame 1024×512 GIF export and frame timing, native PNG-frame save, original draft reload',flush=True);b.close()
