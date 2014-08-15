package net.vipentti.example.authenticator;

import static android.R.layout.simple_dropdown_item_1line;
import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import net.vipentti.example.Injector;
import net.vipentti.example.R;
import net.vipentti.example.R.id;
import net.vipentti.example.R.layout;
import net.vipentti.example.R.string;
import net.vipentti.example.core.BootstrapService;
import net.vipentti.example.core.Constants;
import net.vipentti.example.core.FacebookUtils;
import net.vipentti.example.core.User;
import net.vipentti.example.events.UnAuthorizedErrorEvent;
import net.vipentti.example.ui.TextWatcherAdapter;
import net.vipentti.example.util.Ln;
import net.vipentti.example.util.SafeAsyncTask;

import com.facebook.Session;
import com.github.kevinsawicki.wishlist.Toaster;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.entities.Profile;
import com.sromku.simple.fb.listeners.OnLoginListener;
import com.sromku.simple.fb.listeners.OnLogoutListener;
import com.sromku.simple.fb.listeners.OnProfileListener;

import javax.inject.Inject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.InjectView;
import butterknife.Views;
import retrofit.RetrofitError;

/**
 * Activity to authenticate the user against an API (example API on Parse.com)
 */
public class BootstrapAuthenticatorActivity extends ActionBarAccountAuthenticatorActivity {
    private static final String LOG_TAG = BootstrapAuthenticatorActivity.class.getSimpleName();

    /**
     * PARAM_CONFIRM_CREDENTIALS
     */
    public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

    /**
     * PARAM_PASSWORD
     */
    public static final String PARAM_PASSWORD = "password";

    /**
     * PARAM_USERNAME
     */
    public static final String PARAM_USERNAME = "username";

    /**
     * PARAM_AUTHTOKEN_TYPE
     */
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";


    private AccountManager accountManager;

    @Inject BootstrapService bootstrapService;
    @Inject Bus bus;

    @InjectView(id.et_email) protected AutoCompleteTextView emailText;
    @InjectView(id.et_password) protected EditText passwordText;
    @InjectView(id.b_signin) protected Button signInButton;
    @InjectView(id.btn_login_with_fb) protected Button mLoginButton;

    private final TextWatcher watcher = validationTextWatcher();

    private SafeAsyncTask<Boolean> authenticationTask;
    private String authToken;
    private String authTokenType;

    private SimpleFacebook mSimpleFacebook;

    /**
     * If set we are just checking that the user knows their credentials; this
     * doesn't cause the user's password to be changed on the device.
     */
    private Boolean confirmCredentials = false;

    private String email;

    private String password;


    /**
     * In this instance the token is simply the sessionId returned from Parse.com. This could be a
     * oauth token or some other type of timed token that expires/etc. We're just using the parse.com
     * sessionId to prove the example of how to utilize a token.
     */
    private String token;

    /**
     * Was the original caller asking for an entirely new account?
     */
    protected boolean requestNewAccount = false;

    protected boolean mFacebookLogin = false;

    /**
     * Print hash key
     */
    public static void printHashKey(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                //Log.d(TAG, "keyHash: " + keyHash);
                Ln.d("keyHash: %s", keyHash);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Injector.inject(this);

        accountManager = AccountManager.get(this);

        final Intent intent = getIntent();
        email = intent.getStringExtra(PARAM_USERNAME);
        authTokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
        confirmCredentials = intent.getBooleanExtra(PARAM_CONFIRM_CREDENTIALS, false);

        requestNewAccount = email == null;

        setContentView(layout.login_activity);

        Views.inject(this);

        emailText.setAdapter(new ArrayAdapter<String>(this,
                simple_dropdown_item_1line, userEmailAccounts()));

        //Utils.getHashKey(this);
        //printHashKey(this);

        passwordText.setOnKeyListener(new OnKeyListener() {

            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                if (event != null && ACTION_DOWN == event.getAction()
                        && keyCode == KEYCODE_ENTER && signInButton.isEnabled()) {
                    handleLogin(signInButton);
                    return true;
                }
                return false;
            }
        });

        passwordText.setOnEditorActionListener(new OnEditorActionListener() {

            public boolean onEditorAction(final TextView v, final int actionId,
                                          final KeyEvent event) {
                if (actionId == IME_ACTION_DONE && signInButton.isEnabled()) {
                    handleLogin(signInButton);
                    return true;
                }
                return false;
            }
        });

        emailText.addTextChangedListener(watcher);
        passwordText.addTextChangedListener(watcher);

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("BootstrapAuth", "Logging with facebook");

                if (!mFacebookLogin) {
                    mSimpleFacebook.login(new OnLoginListener() {
                        @Override
                        public void onLogin() {
                            // change the state of the button or do whatever you want
                            final Session session = mSimpleFacebook.getSession();
                            final Date date = session.getExpirationDate();

                            final String dateText = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(date);

                            Ln.d("Logged in %s %s", dateText, session.getAccessToken());
                            mLoginButton.setText("Logout from Facebook");
                            mFacebookLogin = mSimpleFacebook.isLogin();

                            //If we are logged in
                            if (mFacebookLogin) {
                                Profile.Properties properties = new Profile.Properties.Builder()
                                        .add(Profile.Properties.ID)
                                        .add(Profile.Properties.EMAIL)
                                        .add(Profile.Properties.FIRST_NAME)
                                        .add(Profile.Properties.LAST_NAME)
                                        .build();

                                //We want some extra user information from the profile.
                                mSimpleFacebook.getProfile(properties, new OnProfileListener() {
                                    @Override
                                    public void onComplete(final Profile response) {
                                        //super.onComplete(response);
                                        Ln.d("onComplete(): %s %s" , response.getId(), response.getEmail());

                                        final FacebookUtils.PostBody postBody =
                                                FacebookUtils.PostBody.createFacebookBody(
                                                        response.getId(),
                                                        session.getAccessToken(),
                                                        dateText);

                                        authenticationTask = new SafeAsyncTask<Boolean>() {
                                            public Boolean call() throws Exception {
                                                User loginResponse = bootstrapService.authenticate(postBody);

                                                token = loginResponse.getSessionToken();

                                                email = response.getEmail();

                                                return true;
                                            }

                                            @Override protected void onException(final Exception e) throws RuntimeException {
                                                // Retrofit Errors are handled inside of the {
                                                if(!(e instanceof RetrofitError)) {
                                                    final Throwable cause = e.getCause() != null ? e.getCause() : e;
                                                    Ln.e(cause);
                                                    if(cause != null) {
                                                        Toaster.showLong(BootstrapAuthenticatorActivity.this, cause.getMessage());
                                                    }
                                                }
                                            }

                                            @Override public void onSuccess(final Boolean authSuccess) {
                                                onAuthenticationResult(authSuccess);
                                            }

                                            @Override protected void onFinally() throws RuntimeException {
                                                hideProgress();
                                                authenticationTask = null;
                                            }
                                        };
                                        authenticationTask.execute();
                                    }

                                    @Override
                                    public void onFail(String reason) {
                                        //super.onFail(reason);
                                        Ln.d("onFail(): %s", reason);
                                    }

                                    @Override
                                    public void onThinking() {
                                        Ln.d("onThinking()");
                                    }

                                    @Override
                                    public void onException(Throwable throwable) {
                                        Ln.e(throwable);
                                    }
                                });
                            } else {
                                Ln.e("ERROR, NOT LOGGED IN.");
                            }
                        }

                        @Override public void onNotAcceptingPermissions(Permission.Type type) {}

                        @Override public void onThinking() {}

                        @Override public void onException(Throwable throwable) {
                            Ln.d(throwable);
                        }

                        @Override public void onFail(String s) {
                            Ln.d("onFail(): %s", s);
                        }
                    });
                } else {
                    mSimpleFacebook.logout(new OnLogoutListener() {
                        @Override
                        public void onLogout() {
                            // change the state of the button or do whatever you want
                            Ln.d(LOG_TAG, "Logged out");
                            mLoginButton.setText("Login with Facebook");
                            mFacebookLogin = false;
                        }

                        @Override public void onThinking() {}
                        @Override public void onException(Throwable throwable) {}
                        @Override public void onFail(String s) {}
                    });
                }
            }
        });

        final TextView signUpText = (TextView) findViewById(id.tv_signup);
        signUpText.setMovementMethod(LinkMovementMethod.getInstance());
        signUpText.setText(Html.fromHtml(getString(string.signup_link)));
    }


    private List<String> userEmailAccounts() {
        final Account[] accounts = accountManager.getAccountsByType("com.google");
        final List<String> emailAddresses = new ArrayList<String>(accounts.length);
        for (final Account account : accounts) {
            emailAddresses.add(account.name);
        }
        return emailAddresses;
    }

    private TextWatcher validationTextWatcher() {
        return new TextWatcherAdapter() {
            public void afterTextChanged(final Editable gitDirEditText) {
                updateUIWithValidation();
            }

        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        bus.register(this);
        updateUIWithValidation();

        mSimpleFacebook = SimpleFacebook.getInstance(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bus.unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mSimpleFacebook.onActivityResult(this, requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateUIWithValidation() {
        final boolean populated = populated(emailText) && populated(passwordText);
        signInButton.setEnabled(populated);
    }

    private boolean populated(final EditText editText) {
        return editText.length() > 0;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getText(string.message_signing_in));
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(final DialogInterface dialog) {
                if (authenticationTask != null) {
                    authenticationTask.cancel(true);
                }
            }
        });
        return dialog;
    }

    @Subscribe
    public void onUnAuthorizedErrorEvent(UnAuthorizedErrorEvent unAuthorizedErrorEvent) {
        // Could not authorize for some reason.
        Toaster.showLong(BootstrapAuthenticatorActivity.this, R.string.message_bad_credentials);
    }

    /**
     * Handles onClick event on the Submit button. Sends username/password to
     * the server for authentication.
     * <p/>
     * Specified by android:onClick="handleLogin" in the layout xml
     *
     * @param view
     */
    public void handleLogin(final View view) {
        if (authenticationTask != null) {
            return;
        }

        if (requestNewAccount) {
            email = emailText.getText().toString();
        }

        password = passwordText.getText().toString();
        showProgress();

        authenticationTask = new SafeAsyncTask<Boolean>() {
            public Boolean call() throws Exception {

                final String query = String.format("%s=%s&%s=%s",
                        PARAM_USERNAME, email, PARAM_PASSWORD, password);

                User loginResponse = bootstrapService.authenticate(email, password);
                token = loginResponse.getSessionToken();

                return true;
            }

            @Override
            protected void onException(final Exception e) throws RuntimeException {
                // Retrofit Errors are handled inside of the {
                if(!(e instanceof RetrofitError)) {
                    final Throwable cause = e.getCause() != null ? e.getCause() : e;
                    if(cause != null) {
                        Toaster.showLong(BootstrapAuthenticatorActivity.this, cause.getMessage());
                    }
                }
            }

            @Override
            public void onSuccess(final Boolean authSuccess) {
                onAuthenticationResult(authSuccess);
            }

            @Override
            protected void onFinally() throws RuntimeException {
                hideProgress();
                authenticationTask = null;
            }
        };
        authenticationTask.execute();
    }

    /**
     * Called when response is received from the server for confirm credentials
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller.
     *
     * @param result
     */
    protected void finishConfirmCredentials(final boolean result) {
        final Account account = new Account(email, Constants.Auth.BOOTSTRAP_ACCOUNT_TYPE);
        accountManager.setPassword(account, password);

        final Intent intent = new Intent();
        intent.putExtra(KEY_BOOLEAN_RESULT, result);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Called when response is received from the server for authentication
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller. Also sets
     * the authToken in AccountManager for this account.
     */

    protected void finishLogin() {
        final Account account = new Account(email, Constants.Auth.BOOTSTRAP_ACCOUNT_TYPE);

        if (requestNewAccount) {
            accountManager.addAccountExplicitly(account, password, null);
        } else {
            accountManager.setPassword(account, password);
        }

        authToken = token;

        final Intent intent = new Intent();
        intent.putExtra(KEY_ACCOUNT_NAME, email);
        intent.putExtra(KEY_ACCOUNT_TYPE, Constants.Auth.BOOTSTRAP_ACCOUNT_TYPE);

        if (authTokenType != null
                && authTokenType.equals(Constants.Auth.AUTHTOKEN_TYPE)) {
            intent.putExtra(KEY_AUTHTOKEN, authToken);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Hide progress dialog
     */
    @SuppressWarnings("deprecation")
    protected void hideProgress() {
        dismissDialog(0);
    }

    /**
     * Show progress dialog
     */
    @SuppressWarnings("deprecation")
    protected void showProgress() {
        showDialog(0);
    }

    /**
     * Called when the authentication process completes (see attemptLogin()).
     *
     * @param result
     */
    public void onAuthenticationResult(final boolean result) {
        if (result) {
            if (!confirmCredentials) {
                finishLogin();
            } else {
                finishConfirmCredentials(true);
            }
        } else {
            Ln.d("onAuthenticationResult: failed to authenticate");
            if (requestNewAccount) {
                Toaster.showLong(BootstrapAuthenticatorActivity.this,
                        string.message_auth_failed_new_account);
            } else {
                Toaster.showLong(BootstrapAuthenticatorActivity.this,
                        string.message_auth_failed);
            }
        }
    }
}
