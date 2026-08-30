config.devServer = config.devServer || {};
config.devServer.headers = config.devServer.headers || {};
config.devServer.headers["Cross-Origin-Opener-Policy"] = "unsafe-none";
config.devServer.headers["Cross-Origin-Embedder-Policy"] = "unsafe-none";
config.devServer.headers["Access-Control-Allow-Origin"] = "*";
config.devServer.headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, PATCH, OPTIONS";
config.devServer.headers["Access-Control-Allow-Headers"] = "X-Requested-With, content-type, Authorization, Range, If-Range";
config.devServer.headers["Access-Control-Expose-Headers"] = "Content-Range, Accept-Ranges, Content-Length";
