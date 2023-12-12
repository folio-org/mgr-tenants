package org.folio.tm.integration.okapi;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OkapiHeaders {

  /**
   * X-Okapi-Token. A token that identifies the user who is making the current request. May carry additional permissions
   * and other stuff related to authorization. Only the authorization modules should look inside. For the rest of the
   * system this should be opaque. When a module needs to make a call to another module, it needs to pass the token it
   * received in its request into the request to the new module.
   */
  public static final String TOKEN = "X-Okapi-Token";

  /**
   * X-Okapi-User-Id. Tells the user id of the logged-in user. Modules can pass
   * this around, but that is not necessary if we have a good token,
   * mod-authtoken extracts the userId from the token and returns it to Okapi,
   * and Okapi passes it to all modules it invokes.
   */
  public static final String USER_ID = "x-okapi-user-id";
}
