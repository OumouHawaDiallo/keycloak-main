package org.keycloak.testsuite.springboot;

import org.eclipse.persistence.annotations.BatchFetch;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;

import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

public class OfflineTokenSpringBootTest extends AbstractSpringBootTest {
    private static final String SERVLET_URL = BASE_URL + "/TokenServlet";
    private final String TEST_REALM = "test";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    private OAuthGrantPage oauthGrantPage;

    @Before
    public void setUpAuthRealm() {
        testRealmLoginPage.setAuthRealm(REALM_NAME);
    }

    @Test
    public void testTokens() {
        String servletUri = UriBuilder.fromUri(SERVLET_URL)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD);

        tokenPage.assertIsCurrent();

        assertThat(tokenPage.getRefreshToken().getType(), is(equalTo(TokenUtil.TOKEN_TYPE_OFFLINE)));
        assertThat(tokenPage.getRefreshToken().getExpiration(), is(equalTo(0)));

        String accessTokenId = tokenPage.getAccessToken().getId();
        String refreshTokenId = tokenPage.getRefreshToken().getId();

        setAdapterAndServerTimeOffset(19999, SERVLET_URL);

        driver.navigate().to(SERVLET_URL);
        waitForPageToLoad();

        tokenPage.assertIsCurrent();
        assertThat(tokenPage.getRefreshToken().getId(), is(not(equalTo(refreshTokenId))));
        assertThat(tokenPage.getAccessToken().getId(), is(not(equalTo(accessTokenId))));

        setAdapterAndServerTimeOffset(0, SERVLET_URL);

        logout(SERVLET_URL);
        waitForPageToLoad();
        assertCurrentUrlStartsWith(testRealmLoginPage);
    }

    @Test
    public void testRevoke() {
        // Login to servlet first with offline token
        String servletUri = UriBuilder.fromUri(SERVLET_URL)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString();
        driver.navigate().to(servletUri);
        waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage);

        testRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD);
        tokenPage.assertIsCurrent();

        assertThat(tokenPage.getRefreshToken().getType(), is(equalTo(TokenUtil.TOKEN_TYPE_OFFLINE)));

        // Assert refresh works with increased time
        setAdapterAndServerTimeOffset(9999, SERVLET_URL);

        driver.navigate().to(SERVLET_URL);
        waitForPageToLoad();
        tokenPage.assertIsCurrent();

        setAdapterAndServerTimeOffset(0, SERVLET_URL);

        events.clear();

        // Go to account service and revoke grant
        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST_REALM), USER_LOGIN);
        String grantValue = String.valueOf(((LinkedHashMap) ((ArrayList) userConsents.get(0).get("additionalGrants")).get(0)).get("key"));
        assertThat(userConsents, hasSize(1));
        Assert.assertEquals("Offline Token", grantValue);

        AccountHelper.revokeConsents(adminClient.realm(TEST_REALM), USER_LOGIN, CLIENT_ID);
        userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST_REALM), USER_LOGIN);
        Assert.assertEquals(userConsents.size(), 0);

        UserRepresentation userRepresentation =
               ApiUtil.findUserByUsername(realmsResouce().realm(REALM_NAME), USER_LOGIN);
        assertThat(userRepresentation, is(notNullValue()));

        // Assert refresh doesn't work now (increase time one more time)
        setAdapterAndServerTimeOffset(19999, SERVLET_URL);
        driver.navigate().to(SERVLET_URL);
        waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD);
        tokenPage.assertIsCurrent();

        setAdapterAndServerTimeOffset(0, SERVLET_URL);
        logout(SERVLET_URL);
    }

    @Test
    public void testConsent() {
        ClientManager.realm(adminClient.realm(REALM_NAME)).clientId(CLIENT_ID).consentRequired(true);

        // Assert grant page doesn't have 'Offline Access' role when offline token is not requested
        driver.navigate().to(SERVLET_URL);
        waitForPageToLoad();

        testRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD);
        oauthGrantPage.assertCurrent();
        oauthGrantPage.cancel();

        driver.navigate().to(UriBuilder.fromUri(SERVLET_URL)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS)
                .build().toString());
        waitForPageToLoad();

        testRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD);
        oauthGrantPage.assertCurrent();
        oauthGrantPage.accept();

        tokenPage.assertIsCurrent();
        assertThat(tokenPage.getRefreshToken().getType(), is(equalTo(TokenUtil.TOKEN_TYPE_OFFLINE)));

        List<Map<String, Object>> userConsents = AccountHelper.getUserConsents(adminClient.realm(TEST_REALM), USER_LOGIN);
        String grantValue = String.valueOf(((LinkedHashMap) ((ArrayList) userConsents.get(0).get("additionalGrants")).get(0)).get("key"));
        Assert.assertTrue(((List) userConsents.get(0).get("grantedClientScopes")).stream().anyMatch(p -> p.equals("offline_access")));
        Assert.assertEquals("Offline Token", grantValue);

        //This was necessary to be introduced, otherwise other testcases will fail
        logout(SERVLET_URL);
        assertCurrentUrlStartsWith(testRealmLoginPage);

        events.clear();

        // Revert change
        ClientManager.realm(adminClient.realm(REALM_NAME)).clientId(CLIENT_ID).consentRequired(false);
    }
}
