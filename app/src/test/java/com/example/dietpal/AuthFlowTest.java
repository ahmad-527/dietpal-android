package com.example.dietpal;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.Application;
import android.content.Intent;
import android.os.Looper;
import android.widget.EditText;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@LooperMode(LooperMode.Mode.PAUSED)
public class AuthFlowTest {
    private Application app;
    @Before public void setUp() { app = RuntimeEnvironment.getApplication(); app.deleteDatabase("dietpal.db"); }
    @After public void tearDown() { app.deleteDatabase("dietpal.db"); }

    @Test public void registrationAndLoginOpenHomeWithoutWrongFragmentTransaction() {
        try (ActivityController<Register> registration = Robolectric.buildActivity(Register.class)) {
            Register screen = registration.get();
            screen.authExecutor = Runnable::run;
            registration.setup();
            ((EditText) screen.findViewById(R.id.UsrReg)).setText("portfolio-demo");
            ((EditText) screen.findViewById(R.id.editEmail)).setText("demo@example.com");
            ((EditText) screen.findViewById(R.id.editPassword)).setText("demo-password");
            ((EditText) screen.findViewById(R.id.editPassword2)).setText("demo-password");
            screen.findViewById(R.id.RegisterButton1).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(Login.class.getName(), shadowOf(screen).getNextStartedActivity().getComponent().getClassName());
        }
        try (ActivityController<Login> login = Robolectric.buildActivity(Login.class)) {
            Login screen = login.get();
            screen.authExecutor = Runnable::run;
            login.setup();
            ((EditText) screen.findViewById(R.id.editText)).setText("portfolio-demo");
            ((EditText) screen.findViewById(R.id.editPassword)).setText("demo-password");
            screen.findViewById(R.id.LoginButton).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(MainActivity.class.getName(), shadowOf(screen).getNextStartedActivity().getComponent().getClassName());
        }
        try (ActivityController<MainActivity> main = Robolectric.buildActivity(MainActivity.class).setup()) {
            main.get().getSupportFragmentManager().executePendingTransactions();
            assertTrue(main.get().getSupportFragmentManager().findFragmentById(R.id.frameLayout) instanceof HomeFragment);
            main.get().findViewById(R.id.LogOut).performClick();
            Intent logout = shadowOf(main.get()).getNextStartedActivity();
            assertEquals(Welcome.class.getName(), logout.getComponent().getClassName());
            assertTrue((logout.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0);
        }
    }

    @Test public void incorrectPasswordDoesNotOpenHome() {
        try (DatabaseHelper database = new DatabaseHelper(app)) {
            assertTrue(database.insertData("tester", "tester@example.com", "demo-password"));
        }
        try (ActivityController<Login> login = Robolectric.buildActivity(Login.class)) {
            Login screen = login.get(); screen.authExecutor = Runnable::run; login.setup();
            ((EditText) screen.findViewById(R.id.editText)).setText("tester");
            ((EditText) screen.findViewById(R.id.editPassword)).setText("wrong-password");
            screen.findViewById(R.id.LoginButton).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertNull(shadowOf(screen).getNextStartedActivity());
        }
    }
}
