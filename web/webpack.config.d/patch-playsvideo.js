const path = require('path');

config.module = config.module || {};
config.module.rules = config.module.rules || [];

// Add a custom loader to patch playsvideo's dynamic import
config.module.rules.push({
    test: /wasm-ffmpeg\.js$/,
    use: [
        {
            // __dirname is build/js/packages/Avalonmediacard-web/ or build/wasm/...
            // we need to resolve the loader file we created in web/
            loader: path.resolve(__dirname, '../../../../web/playsvideo-loader.js')
        }
    ]
});

// Fix webpack 5: Node.js polyfills
config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};
config.resolve.fallback['os'] = false;
config.resolve.fallback['path'] = false;

// Fix playsvideo export for browser conditions
config.resolve.conditionNames = ['browser', 'module', 'import', 'require', 'default'];
