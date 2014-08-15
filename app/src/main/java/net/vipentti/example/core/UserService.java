package net.vipentti.example.core;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * User service for connecting the the REST API and
 * getting the users.
 */
public interface UserService {

    @GET(Constants.Http.URL_USERS_FRAG)
    UsersWrapper getUsers();

    @PUT(Constants.Http.URL_USERS_FRAG + "/{id}")
    String updateUser(@Header(Constants.Http.HEADER_PARSE_SESSION_TOKEN) String token, @Path("id") String id, @Body User user);

    /**
     * The {@link retrofit.http.Query} values will be transform into query string paramters
     * via Retrofit
     *
     * @param email The users email
     * @param password The users password
     * @return A login response.
     */
    @GET(Constants.Http.URL_AUTH_FRAG)
    User authenticate(@Query(Constants.Http.PARAM_USERNAME) String email,
                               @Query(Constants.Http.PARAM_PASSWORD) String password);


    @POST(Constants.Http.URL_USERS_FRAG)
    User authenticate(@Body FacebookUtils.PostBody body);
}
