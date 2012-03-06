import org.springframework.web.filter.DelegatingFilterProxy
import grails.util.Environment
import com.monochromeroad.grails.plugins.cookiesession.SessionProxyFilter

class CookieSessionGrailsPlugin {
    // the plugin version
    def version = "0.1.2"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2.4 > *"

    def title = "Cookie Session Plugin" // Headline display name of the plugin
    def author = "Masatoshi Hayashi"
    def authorEmail = "literalice@monochromeroad.com"
    def description = "This plugin allow you to store session data in a cookie"

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/cookie-session"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Online location of the plugin's browseable source code.
    def scm = [url: 'https://github.com/literalice/grails-cookie-session']

    def getWebXmlFilterOrder() {
        // make sure the filter is first
        [sessionProxyFilter: -100]
    }

    def doWithWebDescriptor = { xml ->
        if (!isEnabled(application.config)) {
            return
        }

        // add the filter after the last context-param
        def contextParam = xml.'context-param'

        contextParam[contextParam.size() - 1] + {
            'filter' {
                'filter-name'('sessionProxyFilter')
                'filter-class'(DelegatingFilterProxy.name)
            }
        }

        def filter = xml.'filter'
        filter[filter.size() - 1] + {
            'filter-mapping' {
                'filter-name'('sessionProxyFilter')
                'url-pattern'('/*')
            }
        }
    }

    def doWithSpring = {
        if (!isEnabled(application.config)) {
            return
        }

        sessionProxyFilter(SessionProxyFilter) {
            grailsApplication = ref("grailsApplication")
        }
    }

    private boolean isEnabled(config) {
        def enabled = config.grails.plugin.cookiesession.enabled
        if (enabled instanceof Boolean) {
            return enabled
        }
        return !Environment.isDevelopmentMode()
    }

}
