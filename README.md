# The cookie based session storage for Grails

This is the grails plugin that allows you to store the session data in a cookie like Rails or Play!.<br />
It makes a grails application more stateless. So you could more easily scale the application on a clustered environment (including some cloud platforms like Heroku).

# Installation

You can install the plugin by the grails `install-plugin` command.

`grails install-plugin cookie-session`


# Issues

## Replay attacks

You should be aware of the replay attacks when you use the cookie based session store.<br />
Even if someone sniffs a user's cookie, and replay the cookie to your application, the application cannot detect this. (they may log in to your application or ...).

## Session data size

All the session data will be stored in a cookie data. so the size must be up to 4kb.

# Configuration

The plugin can be configured in "Config.groovy".

## Parameters

<table>
  <thead>
    <tr>
      <th>name</th>
      <th>default</th>
      <th>description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
       <td>grails.plugin.cookiesession.enabled</td>
       <td>Development Mode: false, The others: true</td>
       <td>If false, the plugin won't be loaded.</td>
    </tr>
    <tr>
       <td>grails.plugin.cookiesession.id</td>
       <td>gsession</td>
       <td>The cookie's name used for storing session data.</td>
    </tr>
    <tr>
       <td>grails.plugin.cookiesession.timeout</td>
       <td>30</td>
       <td>Session timeout (minutes)</td>
    </tr>
    <tr>
       <td>grails.plugin.cookiesession.hmac.secret</td>
       <td>- <strong>(Required)</strong></td>
       <td>A secret key used for preventing a session cookie from being forged. It should be kept private and unique.</td>
    </tr>
    <tr>
       <td>grails.plugin.cookiesession.hmac.id</td>
       <td>gsesshmac</td>
       <td>The cookie's name used for storing a session HMAC.</td>
    </tr>
    <tr>
       <td>grails.plugin.cookiesession.hmac.algorithm</td>
       <td>HmacSHA1</td>
       <td>An algorithm used for an HMAC.</td>
    </tr>
  </tbody>
</table>

## Example

Config.groovy

    grails.plugin.cookiesession.enabled = true
    grails.plugin.cookiesession.id = "grails-session"
    grails.plugin.cookiesession.timeout = 30
    grails.plugin.cookiesession.hmac.id = "grails-session-hmac"
    grails.plugin.cookiesession.hmac.algorithm = "HmacSHA1"
    grails.plugin.cookiesession.hmac.secret = "Please enter your unique secret key!".bytes.encodeBase64(false).toString()
