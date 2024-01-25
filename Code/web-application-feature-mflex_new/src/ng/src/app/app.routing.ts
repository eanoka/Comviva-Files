import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

// Import Containers
import { DefaultLayoutComponent } from './containers';

import { P404Component } from './views/error/404.component';
import { P500Component } from './views/error/500.component';
import { NewPasswordComponent } from './views/new-password/new-password.component';
import { OtpPwdComponent } from './views/otp-pwd/otp-pwd.component';
import { ChangePasswordComponent } from './views/change-password/change-password.component';
import { ForgotpasswordComponent } from './views/forgotpassword/forgotpassword.component';
import { OtpAuthComponent } from './views/otp-auth/otp-auth.component';
import { LoginComponent } from './views/login/login.component';
export const routes: Routes = [
  { path: 'new-password', component: NewPasswordComponent },
  { path: 'otp-pwd', component: OtpPwdComponent },
  { path: 'change-password', component: ChangePasswordComponent },
  {
    path: 'forgotpassword',
    component: ForgotpasswordComponent
  },
  {
    path: 'otp-auth',
    component: OtpAuthComponent
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: '404',
    component: P404Component,
    data: {
      title: 'Page 404'
    }
  },
  {
    path: '500',
    component: P500Component,
    data: {
      title: 'Page 500'
    }
  },
  {
    path: '',
    component: DefaultLayoutComponent,
    data: {
      title: 'Home'
    },
    children: [
      {
        path: 'manage-bill-data',
        loadChildren: () => import('./views/manage-bill-data/manage-bill-data.module').then(m => m.ManageBillDataModule)
      },
      {
        path: 'payment-request',
        loadChildren: () => import('./views/payment-request/payment-request.module').then(m => m.PaymentRequestModule)
      },
      {
        path: 'payment-approval',
        loadChildren: () => import('./views/payment-approval/payment-approval.module').then(m => m.PaymentApprovalModule)
      },
      {
        path: 'payment',
        loadChildren: () => import('./views/payment/payment.module').then(m => m.PaymentModule)
      },
      {
        path: 'user-management',
        loadChildren: () => import('./views/user-management/user-management.module').then(m => m.UserManagementModule)
      },
      {
        path: 'account-management',
        loadChildren: () => import('./views/account-management/component-group.module').then(m => m.AccountManagementModule)
      },
      {
        path: 'role-management',
        loadChildren: () => import('./views/role-management/role-management.module').then(m => m.RoleManagementModule)
      },
      {
        path: 'transaction-reports',
        loadChildren: () => import('./views/transaction-reports/transaction-reports.module').then(m => m.TransactionReportsModule)
      },
      {
        path: 'dashboard',
        loadChildren: () => import('./views/dashboard/dashboard.module').then(m => m.DashboardModule)
      }
    ]
  },
  { path: '**', component: P404Component }
];

@NgModule({
  imports: [ RouterModule.forRoot(routes) ],
  exports: [ RouterModule ]
})
export class AppRoutingModule {}
