package client.app;

import client.data_access.password_checker.PasswordCheckerDataAccess;
import client.entity.CommonUserFactory;
import client.entity.UserFactory;
import client.interface_adapter.ViewManagerModel;
import client.interface_adapter.login.LoginViewModel;
import client.interface_adapter.main.MainViewModel;
import client.interface_adapter.signup.SignupController;
import client.interface_adapter.signup.SignupPresenter;
import client.interface_adapter.signup.SignupViewModel;
import client.use_case.high_contrast.HighContrastDataAccessInterface;
import client.use_case.password_checker.PasswordCheckerInputBoundary;
import client.use_case.password_checker.PasswordCheckerInteractor;
import client.use_case.signup.SignupDataAccessInterface;
import client.use_case.signup.SignupInputBoundary;
import client.use_case.signup.SignupInteractor;
import client.use_case.signup.SignupOutputBoundary;
import client.view.SignupView;

import javax.swing.*;
import java.io.IOException;

public class SignupUseCaseFactory {

    /**
     * Prevent instantiation.
     */
    private SignupUseCaseFactory() {
    }

    public static SignupView create(ViewManagerModel viewManagerModel, MainViewModel mainViewModel, SignupViewModel signupViewModel, LoginViewModel loginViewModel, SignupDataAccessInterface userDataAccessObject, HighContrastDataAccessInterface optionsDataAccessObject) {

        try {
            SignupController signupController = createUserSignupUseCase(viewManagerModel, signupViewModel, mainViewModel, loginViewModel, userDataAccessObject);
            return new SignupView(signupController, signupViewModel, optionsDataAccessObject.get(HighContrastDataAccessInterface.HIGH_CONTRAST));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not open user data file.");
        }

        return null;
    }

    private static SignupController createUserSignupUseCase(ViewManagerModel viewManagerModel, SignupViewModel signupViewModel, MainViewModel mainViewModel, LoginViewModel loginViewModel, SignupDataAccessInterface userDataAccessObject) throws IOException {
        SignupOutputBoundary signupOutputBoundary = new SignupPresenter(viewManagerModel, signupViewModel, mainViewModel, loginViewModel);

        UserFactory userFactory = new CommonUserFactory();

        SignupInputBoundary userSignupInteractor = new SignupInteractor(userDataAccessObject, signupOutputBoundary, userFactory);

        PasswordCheckerInputBoundary passwordCheckerUseCaseInteractor = new PasswordCheckerInteractor(new PasswordCheckerDataAccess());
        return new SignupController(userSignupInteractor, passwordCheckerUseCaseInteractor);
    }
}
