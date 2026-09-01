package org.ensodai.avalonmediacard.presentation.locale

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual fun setAppLocale(language: String) {
    js(
        """{
        try {
            if (!window.__avalonOriginalLanguages) {
                window.__avalonOriginalLanguages = window.navigator.languages ? Array.from(window.navigator.languages) : ['en'];
            }
            const langs = (language === 'auto' || !language) ? window.__avalonOriginalLanguages : [language];
            Object.defineProperty(navigator, 'languages', {
                get: () => langs,
                configurable: true
            });
            Object.defineProperty(navigator, 'language', {
                get: () => langs[0] || 'en',
                configurable: true
            });
        } catch (e) {
            console.error('Failed to set navigator language', e);
        }
    }"""
    )
}
