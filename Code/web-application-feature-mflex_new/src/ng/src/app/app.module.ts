import {BrowserModule} from '@angular/platform-browser';
import {Injector, NgModule} from '@angular/core';
import {HashLocationStrategy, LocationStrategy} from '@angular/common';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

import {PerfectScrollbarModule} from 'ngx-perfect-scrollbar';

import {AppComponent} from './app.component';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';

// Import containers
import {DefaultLayoutComponent} from './containers';

import {P404Component} from './views/error/404.component';
import {P500Component} from './views/error/500.component';
import {AppAsideModule, AppBreadcrumbModule, AppFooterModule, AppHeaderModule, AppSidebarModule} from '@coreui/angular';

// Import routing module
import {AppRoutingModule} from './app.routing';

// Import 3rd party components
import {BsDropdownModule} from 'ngx-bootstrap/dropdown';
import {TabsModule} from 'ngx-bootstrap/tabs';
import {ChartsModule} from 'ng2-charts';
import {SnackbarComponent} from "./views/blocks/snackbar.component";
import {AlertModule} from "ngx-bootstrap/alert";
import {HttpErrorInterceptor} from "./generic/http-error.interceptor";
import {ExtendGlobals} from "./generic/prototype.extensions";
import {ConfirmDialogComponent} from "./views/blocks/modal-confirm.component";
import {ModalModule} from "ngx-bootstrap/modal";


// Import login component
import { LoginComponent } from './views/login/login.component';

// Import forgot password component
import { ForgotpasswordComponent } from './views/forgotpassword/forgotpassword.component';
import { NewPasswordComponent } from './views/new-password/new-password.component';

// Import OTP Authentication component
import { OtpAuthComponent } from './views/otp-auth/otp-auth.component';
import { OtpPwdComponent } from './views/otp-pwd/otp-pwd.component';

// Import change password component
import { ChangePasswordComponent } from './views/change-password/change-password.component';

// Login API service file
import { LoginService } from './views/service/login.service';
import { LoginSharedService } from './views/service/login-shared.service';

// Change password API service file
import { ChangepaswwordService } from './views/service/changepassword.service';

// Forgot password API service file
import { ForgotpasswordService } from './views/service/forgotpassword.service';
import { AuthGuard } from './auth/auth.guard';
import { AuthService } from './views/service/auth.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

const APP_CONTAINERS = [
    DefaultLayoutComponent
];

ExtendGlobals()

export let AppInjector: Injector;
@NgModule({
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        AppRoutingModule,
        AppAsideModule,
        AppBreadcrumbModule.forRoot(),
        AppFooterModule,
        AppHeaderModule,
        AppSidebarModule,
        PerfectScrollbarModule,
        BsDropdownModule.forRoot(),
        TabsModule.forRoot(),
        ChartsModule,
        HttpClientModule,
        AlertModule,
        ModalModule,
        FormsModule,
        ReactiveFormsModule
    ],
    declarations: [
        AppComponent,
        ...APP_CONTAINERS,
        P404Component,
        P500Component,
        SnackbarComponent,
        ConfirmDialogComponent,
        LoginComponent,
        ForgotpasswordComponent,
        OtpAuthComponent,
        OtpPwdComponent,
        ChangePasswordComponent,
        NewPasswordComponent
    ],
    providers: [{
        provide: LocationStrategy,
        useClass: HashLocationStrategy
    }, HttpClientModule, AuthGuard, {
        provide: HTTP_INTERCEPTORS,
        useClass: HttpErrorInterceptor,
        multi: true
    }, LoginService, ChangepaswwordService, ForgotpasswordService, LoginSharedService, AuthService],
    bootstrap: [AppComponent]
})
export class AppModule {
    constructor(private injector: Injector) {
        AppInjector = injector;
    }
}
