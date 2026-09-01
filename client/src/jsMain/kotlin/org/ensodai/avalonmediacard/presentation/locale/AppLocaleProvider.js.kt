package org.ensodai.avalonmediacard.presentation.locale

actual fun setAppLocale(language: String) {
    js(
        """
        try {
            if (!window.__avalonOriginalLanguages) {
                window.__avalonOriginalLanguages = window.navigator.languages ? Array.from(window.navigator.languages) : ['en'];
            }
            var langs = (language === 'auto' || !language) ? window.__avalonOriginalLanguages : [language];
            Object.defineProperty(navigator, 'languages', {
                get: function() { return langs; },
                configurable: true
            });
            Object.defineProperty(navigator, 'language', {
                get: function() { return langs[0] || 'en'; },
                configurable: true
            });
        } catch (e) {
            console.error('Failed to set navigator language', e);
        }
        """
    )
}
