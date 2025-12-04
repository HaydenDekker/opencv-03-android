package com.hdekker.opencv_on_android;

import android.Manifest;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.GrantPermissionRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A custom JUnit TestRule that combines an ActivityScenarioRule for MainActivity
 * with a GrantPermissionRule for the CAMERA permission.
 * <p>
 * This simplifies test setup by providing a single rule to launch the activity
 * with the necessary permissions automatically granted.
 */
public class MainActivityPermissionRule implements TestRule {

    private final ActivityScenarioRule<MainActivity> activityRule;
    private final RuleChain ruleChain;

    public MainActivityPermissionRule() {
        // 1. Define the rule to launch MainActivity
        activityRule = new ActivityScenarioRule<>(MainActivity.class);

        // 2. Define the rule to grant the CAMERA permission
        GrantPermissionRule permissionRule = GrantPermissionRule.grant(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        );

        // 3. Build the RuleChain, ensuring permissions are granted BEFORE the activity starts
        ruleChain = RuleChain
                .outerRule(permissionRule)
                .around(activityRule);
    }

    /**
     * Provides access to the underlying ActivityScenario.
     * @return The ActivityScenario for MainActivity.
     */
    public ActivityScenario<MainActivity> getScenario() {
        return activityRule.getScenario();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        // Delegate the apply call to the internal RuleChain
        return ruleChain.apply(base, description);
    }
}
