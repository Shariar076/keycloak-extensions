package keycloak.requiredaction;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.validation.Validation;

import java.util.function.Consumer;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class MobileNumberAndPurposeRequiredAction implements RequiredActionProvider {

	public static final String PROVIDER_ID = "mobile-number-and-purpose-ra";

	private static final String MOBILE_NUMBER_FIELD = "mobile_number";
	private static final String PURPOSE_FIELD = "purpose";

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
	public void evaluateTriggers(RequiredActionContext context) {
		// you would implement something like the following, if this required action should be "self registering" at the user
		if (context.getUser().getFirstAttribute(MOBILE_NUMBER_FIELD) == null || context.getUser().getFirstAttribute(PURPOSE_FIELD) == null) {
			context.getUser().addRequiredAction(PROVIDER_ID);
		}
	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		// show initial form
		context.challenge(createForm(context, null));
	}

	@Override
	public void processAction(RequiredActionContext context) {
		// submitted form

		UserModel user = context.getUser();

		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

		String mobileNumber = formData.getFirst(MOBILE_NUMBER_FIELD);
		if (Validation.isBlank(mobileNumber) || mobileNumber.length() < 5) {
			context.challenge(createForm(context, form -> form.addError(new FormMessage(MOBILE_NUMBER_FIELD, "Invalid input"))));
			return;
		}

		String purpose = formData.getFirst(PURPOSE_FIELD);
		if (Validation.isBlank(purpose)) {
			context.challenge(createForm(context, form -> form.addError(new FormMessage(PURPOSE_FIELD, "Invalid input"))));
			return;
		}

		user.setSingleAttribute(MOBILE_NUMBER_FIELD, mobileNumber);
		user.setSingleAttribute(PURPOSE_FIELD, purpose);
		user.removeRequiredAction(PROVIDER_ID);

		context.success();
	}

	@Override
	public void close() {
	}

	private Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formConsumer) {
		LoginFormsProvider form = context.form();
		form.setAttribute("first_name", context.getUser().getFirstName());

		String mobileNumber = context.getUser().getFirstAttribute(MOBILE_NUMBER_FIELD);
		form.setAttribute(MOBILE_NUMBER_FIELD, mobileNumber == null ? "" : mobileNumber);

		String purpose = context.getUser().getFirstAttribute(PURPOSE_FIELD);
		form.setAttribute(PURPOSE_FIELD, purpose == null ? "" : purpose);

		if (formConsumer != null) {
			formConsumer.accept(form);
		}

		return form.createForm("update-usage-info.ftl");
	}

}
