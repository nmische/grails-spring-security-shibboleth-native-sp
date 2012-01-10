package edu.umn.shibboleth.sp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.util.Assert;


/**
	Processes a {@link ShibbolethAuthenticationToken}, and authenticates via Shibboleth if available.

	@author <a href="mailto:ajz@umn.edu">Aaron J. Zirbes</a>
*/
class ShibbolethAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	// configuration settings + default values
	private String principalUsernameAttribute = null;
	private String authenticationMethodAttribute = "Shib-AuthnContext-Method";
	private String identityProviderAttribute = "Shib-Identity-Provider";
	private String authenticationInstantAttribute = "Shib-Authentication-Instant";
	private Collection<String> extraAttributes = new ArrayList<String>();

	/** Ensure all configuration settings are set */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		Assert.notNull(principalUsernameAttribute, "principalUsernameAttribute cannot be null");
		Assert.notNull(authenticationMethodAttribute, "authenticationMethodAttribute cannot be null");
		Assert.notNull(identityProviderAttribute, "identityProviderAttribute cannot be null");
		Assert.notNull(authenticationInstantAttribute, "authenticationInstantAttribute cannot be null");
		Assert.notNull(extraAttributes, "extraAttributes cannot be null");
	}

	/** The default constructor */
	public ShibbolethAuthenticationFilter() {
		super("/j_spring_shibboleth_native_sp_security_check");
		logger.debug("instantiation");
	}

	/** Try logging in the user via Shibboleth Native SP */
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {

		Authentication token = null;

		logger.debug("attemptAuthentication():: invocation");

		// These are set by mod_shib22 in Apache and passed through mod_jk 
		// to the servlet (Tomcat, Glassfish, etc..)
		// This means you MUST trust the assertions chain made by mod_jk, and in 
		// turn Apache, and in turn mod_shib22, and in turn the Shibboleth SP (shibd)
		// This is often referred to as "pre-authentication"
		String remoteUser = request.getRemoteUser();
		String remoteAddress = request.getRemoteAddr();
		String authType = request.getAuthType();

		// These are configurable attributes to load
		String authenticationMethod = request.getAttribute(this.authenticationMethodAttribute).toString();
		String identityProvider = request.getAttribute(this.identityProviderAttribute).toString();
		String authenticationInstant = request.getAttribute(this.authenticationInstantAttribute).toString();

		// overwrite the remoteUser if the principalUsernameAttribute was set, and contains a value
		if (request.getAttribute(this.principalUsernameAttribute) != null) {
			remoteUser = request.getAttribute(this.principalUsernameAttribute).toString();
		}

		HashMap<String, String> attributes = new HashMap<String, String>();

		// load any extra attributes
		for (Iterator iter = this.extraAttributes.iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			String val = request.getAttribute(key).toString();
			attributes.put(key, val);
		}

		// INFO: authType is not configurable, as this plugin 
		// is meant to be used with the Shibboleth Native SP that 
		// integrates with Apache
		if (remoteUser == null) {
			logger.debug("remoteUser is null.  No valid shibboleth session found.");
		} else if ( remoteUser.length() <= 0 ) {
			logger.debug("remoteUser is empty.  No valid shibboleth session found.");
		} else if ( authType == null ) {
			logger.debug("authType is null.   No valid shibboleth session found.");
		} else if ( ! authType.equals("shibboleth") ) {
			logger.debug("authType is not 'shibboleth'.  No valid shibboleth session found.");
		} else {
			// create the token
			// principal is set to remoteUser because the default string convert (toString)
			// for the AbstractAuthenticationProcessingFilter class is principal.toString()

			logger.debug("building a shibboleth token");

			ShibbolethAuthenticationToken shibbolethAuthenticationToken = new
				ShibbolethAuthenticationToken(remoteUser, authType, authenticationMethod, 
					identityProvider, authenticationInstant, remoteAddress, attributes);

			logger.debug("calling authenticate()");
			token = this.getAuthenticationManager().authenticate(shibbolethAuthenticationToken);
		}

		return token;
	}

	public void setPrincipalUsernameAttribute(final String principalUsernameAttribute) {
	   this.principalUsernameAttribute = principalUsernameAttribute;
	}

	public void setAuthenticationMethodAttribute(final String authenticationMethodAttribute) {
	   this.authenticationMethodAttribute = authenticationMethodAttribute;
	}

	public void setIdentityProviderAttribute(final String identityProviderAttribute) {
	   this.identityProviderAttribute = identityProviderAttribute;
	}

	public void setAuthenticationInstantAttribute(final String authenticationInstantAttribute) {
	   this.authenticationInstantAttribute = authenticationInstantAttribute;
	}

	public void setExtraAttributes(final Collection<String> extraAttributes) {
	   this.extraAttributes = extraAttributes;
	}
}
