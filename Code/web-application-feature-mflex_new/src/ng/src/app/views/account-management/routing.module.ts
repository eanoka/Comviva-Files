import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { BarUnbarAccountComponent } from './bar-unbar-account.component';
import { CheckBalanceComponent } from './check-balance.component';
import { BillPaymentApprovalComponent } from "./billpayment-approval.component";
import { BillDataValidatorComponent } from "./billdata-validator.component";
import { CreateAccountComponent } from './create-account.component';

const routes: Routes = [
  {
    path: '',
    data: {
      title: 'account-management'
    },
    children: [
      {
        path: '',
        redirectTo: 'account-management'
      },
      {
        path: 'bar-unbar-account',
        component: BarUnbarAccountComponent,
        data: {
          title: 'bar-unbar-account'
        }
      },
      {
        path: 'check-balance',
        component: CheckBalanceComponent,
        data: {
          title: 'check-balance'
        }
      },
      {
	    path: 'billpayment-approval',
        component: BillPaymentApprovalComponent,
        data: {
          title: 'billpayment-approval'
        }
      },
      {
        path: 'billdata-validator',
        component: BillDataValidatorComponent,
        data: {
          title: 'billdata-validator'
        }
      },
      {
        path: 'create-account',
        component: CreateAccountComponent,
        data: {
          title: 'create-account'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class RoutingModule {}
