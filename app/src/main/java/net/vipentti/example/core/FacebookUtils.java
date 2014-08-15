package net.vipentti.example.core;

/**
 * Created by Ville Penttinen <villem.penttinen@gmail.com>
 *
 * @author Ville Penttinen <villem.penttinen@gmail.com>
 */
public final class FacebookUtils {
    public static class PostBody {
        public final AuthData authData;

        public PostBody(AuthData authData) {
            this.authData = authData;
        }

        public static PostBody createFacebookBody(String id, String access_token, String expiration_date) {
            FacebookAuthData data = new FacebookAuthData(id, access_token, expiration_date);
            AuthData auth = new AuthData(data);
            return new PostBody(auth);
        }
    }

    public static class AuthData {
        public final FacebookAuthData facebook;

        public AuthData(FacebookAuthData facebook) {
            this.facebook = facebook;
        }
    }

    public static class FacebookAuthData {
        public final String id;
        public final String access_token;
        public final String expiration_date;

        public FacebookAuthData(String id, String access_token, String expiration_date) {
            this.id = id;
            this.access_token = access_token;
            this.expiration_date = expiration_date;
        }
    }
}
