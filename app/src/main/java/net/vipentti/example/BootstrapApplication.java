

package net.vipentti.example;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;

import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;

import net.vipentti.example.core.Constants;

/**
 * ExampleApp application
 */
public class BootstrapApplication extends Application {

    private static BootstrapApplication instance;

    /**
     * Create main application
     */
    public BootstrapApplication() {
    }

    /**
     * Create main application
     *
     * @param context
     */
    public BootstrapApplication(final Context context) {
        this();
        attachBaseContext(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        // Perform injection
        Injector.init(getRootModule(), this);

        //Set up some SimpleFacebook Permissions.
        Permission[] permissions = new Permission[] {
                Permission.PUBLIC_PROFILE,
                Permission.EMAIL,
        };

        SimpleFacebookConfiguration configuration = new SimpleFacebookConfiguration.Builder()
                .setAppId(Constants.Facebook.APP_ID)
                .setNamespace(Constants.Facebook.NAMESPACE)
                .setPermissions(permissions)
                .build();

        SimpleFacebook.setConfiguration(configuration);
    }

    private Object getRootModule() {
        return new RootModule();
    }


    /**
     * Create main application
     *
     * @param instrumentation
     */
    public BootstrapApplication(final Instrumentation instrumentation) {
        this();
        attachBaseContext(instrumentation.getTargetContext());
    }

    public static BootstrapApplication getInstance() {
        return instance;
    }
}
