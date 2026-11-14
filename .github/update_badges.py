#!/usr/bin/env python3
import json, os, re, subprocess, urllib.request

BG = "#5F5E5E"
FG = "#FFFFFF"
GITHUB_ICON = """<path fill-rule="evenodd" clip-rule="evenodd" d="M23.4075 6C18.4266 6 14.4 10.125 14.4 15.2281C14.4 19.3074 16.98 22.7604 20.5591 23.9825C21.0065 24.0744 21.1705 23.7839 21.1705 23.5396C21.1705 23.3257 21.1557 22.5924 21.1557 21.8283C18.65 22.3784 18.1283 20.7283 18.1283 20.7283C17.7256 19.6588 17.129 19.3839 17.129 19.3839C16.3089 18.8186 17.1887 18.8186 17.1887 18.8186C18.0984 18.8797 18.5757 19.7658 18.5757 19.7658C19.3809 21.1713 20.6784 20.7742 21.2003 20.5297C21.2748 19.9338 21.5136 19.5213 21.7671 19.2922C19.7687 19.0783 17.666 18.2838 17.666 14.7392C17.666 13.7308 18.0237 12.9058 18.5905 12.2642C18.5011 12.035 18.1878 11.0876 18.6801 9.81955C18.6801 9.81955 19.4406 9.57505 21.1555 10.7668C21.8897 10.5648 22.6469 10.462 23.4075 10.4612C24.168 10.4612 24.9433 10.5682 25.6592 10.7668C27.3743 9.57505 28.1348 9.81955 28.1348 9.81955C28.6271 11.0876 28.3137 12.035 28.2243 12.2642C28.806 12.9058 29.1489 13.7308 29.1489 14.7392C29.1489 18.2838 27.0463 19.0629 25.0329 19.2922C25.3611 19.5824 25.6443 20.1324 25.6443 21.0033C25.6443 22.2408 25.6296 23.234 25.6296 23.5394C25.6296 23.7839 25.7937 24.0744 26.2409 23.9827C29.82 22.7602 32.4 19.3074 32.4 15.2281C32.4147 10.125 28.3734 6 23.4075 6Z" fill="white"/>"""
GIT_ICON = """<path d="M32.0393 14.1884L24.1897 6.33941C24.0823 6.23181 23.9547 6.14645 23.8142 6.08822C23.6737 6.02998 23.5232 6 23.3711 6C23.219 6 23.0685 6.02998 22.928 6.08822C22.7875 6.14645 22.6599 6.23181 22.5525 6.33941L20.9223 7.96958L22.9896 10.0368C23.2326 9.9543 23.4939 9.94142 23.7439 9.99964C23.9939 10.0579 24.2226 10.1849 24.4041 10.3663C24.5867 10.5491 24.7141 10.7795 24.772 11.0313C24.8299 11.283 24.8158 11.5459 24.7315 11.7901L26.7238 13.7826C26.9679 13.6978 27.231 13.6835 27.4828 13.7413C27.7347 13.7992 27.9652 13.9269 28.1478 14.1097C28.2757 14.2376 28.3772 14.3895 28.4464 14.5566C28.5156 14.7238 28.5513 14.9029 28.5513 15.0838C28.5513 15.2647 28.5156 15.4438 28.4464 15.611C28.3772 15.7781 28.2757 15.9299 28.1478 16.0578C27.8893 16.3162 27.5388 16.4614 27.1733 16.4614C26.8078 16.4614 26.4573 16.3162 26.1989 16.0578C26.0065 15.8656 25.8753 15.6206 25.822 15.3539C25.7686 15.0872 25.7955 14.8106 25.8991 14.5591L24.0404 12.701V17.5918C24.2366 17.6887 24.4069 17.8309 24.5371 18.0067C24.6674 18.1826 24.7539 18.3869 24.7895 18.6028C24.825 18.8187 24.8087 19.04 24.7417 19.2483C24.6747 19.4566 24.5591 19.6459 24.4043 19.8007C24.2764 19.9286 24.1246 20.0301 23.9575 20.0993C23.7903 20.1686 23.6112 20.2042 23.4303 20.2042C23.2494 20.2042 23.0703 20.1686 22.9031 20.0993C22.736 20.0301 22.5841 19.9286 22.4562 19.8007C22.3283 19.6728 22.2268 19.5209 22.1575 19.3537C22.0883 19.1866 22.0526 19.0074 22.0526 18.8265C22.0526 18.6456 22.0883 18.4664 22.1575 18.2993C22.2268 18.1321 22.3283 17.9803 22.4562 17.8524C22.5853 17.723 22.7388 17.6207 22.9078 17.5513V12.6155C22.7388 12.5465 22.5852 12.4444 22.4562 12.3152C22.263 12.1218 22.1317 11.8753 22.0789 11.607C22.0261 11.3388 22.0542 11.0609 22.1596 10.8087L20.1213 8.77039L14.7393 14.1516C14.6318 14.2591 14.5464 14.3868 14.4882 14.5273C14.43 14.6678 14.4 14.8184 14.4 14.9705C14.4 15.1226 14.43 15.2732 14.4882 15.4137C14.5464 15.5542 14.6318 15.6819 14.7393 15.7894L22.5891 23.6394C22.8063 23.8563 23.1007 23.9782 23.4077 23.9782C23.7147 23.9782 24.0091 23.8563 24.2263 23.6394L32.0393 15.8262C32.2562 15.6089 32.378 15.3143 32.378 15.0073C32.378 14.7002 32.2562 14.4057 32.0393 14.1884" fill="white"/>"""
FONT_URL = "https://github.com/googlefonts/opensans/raw/main/fonts/ttf/OpenSans-Bold.ttf"

def api(path):
    req = urllib.request.Request("https://api.github.com" + path, headers={"User-Agent": "badges"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)

def get_repo():
    url = subprocess.run(["git", "config", "--get", "remote.origin.url"], capture_output=True, text=True).stdout.strip()
    m = re.search(r"github\.com[:/]([^/]+/[^/.]+)", url)
    return m.group(1)

def text_paths(value):
    from fontTools.pens.svgPathPen import SVGPathPen
    from fontTools.ttLib import TTFont
    fp = "/tmp/OpenSans-Bold.ttf"
    if not os.path.exists(fp):
        urllib.request.urlretrieve(FONT_URL, fp)
    font = TTFont(fp)
    sc = (14.32 / (font["OS/2"].sCapHeight / font["head"].unitsPerEm)) / font["head"].unitsPerEm
    cmap = font.getBestCmap()
    gs = font.getGlyphSet()
    x, total = 43.44, 0.0
    parts = []
    for ch in value:
        if ord(ch) not in cmap:
            return None, 0
        g = gs[cmap[ord(ch)]]
        pen = SVGPathPen(gs)
        g.draw(pen)
        parts.append('<path transform="translate(%.2f,22) scale(%.6f,-%.6f)" d="%s" fill="%s"/>' % (x, sc, sc, pen.getCommands(), FG))
        x += g.width * sc
        total += g.width * sc
    return parts, round(43.44 + total + 14)

def pill(w):
    r = 8.28427
    return ('<path d="M0 15C0 6.71573 6.71573 0 15 0H%0.5gC%0.5g 0 %0.5g 6.71573 %0.5g 15V15'
            'C%0.5g 23.2843 %0.5g 30 %0.5g 30H15C6.71573 30 0 23.2843 0 15V15Z" fill="%s"/>'
            % (w - 15, w - 15 + r, w, w, w, w - 15 + r, w - 15, BG))

def badge(icon, value):
    parts, w = text_paths(value)
    if parts is None:
        return None
    return ('<svg width="%d" height="30" viewBox="0 0 %d 30" fill="none" xmlns="http://www.w3.org/2000/svg">\n'
            % (w, w)) + pill(w) + "\n" + icon + "\n" + "\n".join(parts) + "\n</svg>\n"

def write(path, content):
    if os.path.exists(path) and open(path).read() == content:
        return False
    open(path, "w").write(content)
    return True

def main():
    repo = get_repo()
    data = api("/repos/" + repo)
    stars = data["stargazers_count"]
    s = "{:,}".format(stars) if stars < 1000 else ("%.1fk" % (stars / 1000)).rstrip("0").rstrip(".")
    changed = False
    svg = badge(GITHUB_ICON, s + " stars")
    if svg:
        changed |= write("assets/badges/stars.svg", svg)
    tags = api("/repos/%s/tags?per_page=100" % repo)
    if tags:
        svg = badge(GIT_ICON, tags[0]["name"])
        if svg:
            changed |= write("assets/badges/release.svg", svg)
    print("updated" if changed else "no changes")

main()
