#!/usr/bin/env python3
"""单文件 HTTP 服务器 — 静态文件 + API 代理"""
import http.server
import urllib.request
import json
import os

PORT = 8888
API_BASE = "http://localhost:8080"
STATIC_DIR = "/root/vehicle-management/client/dist"

class ProxyHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=STATIC_DIR, **kwargs)

    def do_GET(self):
        if self.path.startswith('/api/'):
            self.proxy_request('GET')
        else:
            # SPA fallback: 所有非 API 请求返回 index.html
            if not os.path.exists(STATIC_DIR + self.path) and '.' not in self.path.split('/')[-1]:
                self.path = '/index.html'
            super().do_GET()

    def do_POST(self):
        if self.path.startswith('/api/'):
            self.proxy_request('POST')
        else:
            self.send_error(404)

    def do_PUT(self):
        if self.path.startswith('/api/'):
            self.proxy_request('PUT')
        else:
            self.send_error(404)

    def do_DELETE(self):
        if self.path.startswith('/api/'):
            self.proxy_request('DELETE')
        else:
            self.send_error(404)

    def do_PATCH(self):
        if self.path.startswith('/api/'):
            self.proxy_request('PATCH')
        else:
            self.send_error(404)

    def proxy_request(self, method):
        url = API_BASE + self.path
        body = None
        content_len = int(self.headers.get('Content-Length', 0))
        if content_len:
            body = self.rfile.read(content_len)

        req = urllib.request.Request(url, data=body, method=method)
        for k, v in self.headers.items():
            if k.lower() in ('host', 'content-length'):
                continue
            req.add_header(k, v)

        try:
            resp = urllib.request.urlopen(req)
            self.send_response(resp.status)
            for k, v in resp.getheaders():
                if k.lower() == 'transfer-encoding':
                    continue
                self.send_header(k, v)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(resp.read())
        except urllib.error.HTTPError as e:
            self.send_response(e.code)
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(e.read())

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,PATCH,OPTIONS')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.end_headers()

if __name__ == '__main__':
    server = http.server.HTTPServer(('0.0.0.0', PORT), ProxyHandler)
    print(f'Server running on http://0.0.0.0:{PORT}')
    server.serve_forever()
