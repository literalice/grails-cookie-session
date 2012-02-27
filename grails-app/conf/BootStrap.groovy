import com.example.User
import com.example.Role
import com.example.UserRole

class BootStrap {

    def init = { servletContext ->
        def role = new Role(authority: "ROLE_USER").save(failOnError: true, flush: true)
        def username = "masatoshi"
        def user = new User(username: username,
                 password: username, enabled: true).save(failOnError: true, flush: true)
        UserRole.create(user, role, true)
    }

}
