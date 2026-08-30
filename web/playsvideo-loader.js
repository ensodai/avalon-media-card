module.exports = function (source) {
    // Replace Vite's ignore comment with Webpack's ignore comment
    // so Webpack leaves the dynamic import() alone as a native browser import.
    return source.replace(
        'import(/* @vite-ignore */ coreURL)',
        'import(/* webpackIgnore: true */ coreURL)'
    );
};
