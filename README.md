# Grails Plugin for storing session data in cookie

This is the grails plugin that allows you to store session data in cookie like Rails or Play!.

# Installation

Clone the plugin's source, and use it as a grails inline plugin.

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
       <td>grails.plugin.cookiesession.hmac.secret</td>
       <td>-</td>
       <td>A secret key used for calculation of an HMAC. This value is required. Be sure to make this secret key unique for every application.</td>
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
       <td>grails.plugin.cookiesession.hmac.id</td>
       <td>gsesshmac</td>
       <td>The cookie's name used for storing session an HMAC.</td>
    </tr>
    <tr>
       <td>grails.plugin.cookiesession.hmac.algorithm</td>
       <td>HmacSHA1</td>
       <td>An algorithm name used for calculation of an HMAC.</td>
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
