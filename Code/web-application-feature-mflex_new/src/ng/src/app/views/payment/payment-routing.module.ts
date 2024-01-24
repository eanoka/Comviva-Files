import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { ApprovedBillPayStatusComponent } from './approved-bill-pay-status.component';
import { BillPaymentComponent } from './bill-payment.component';

const routes: Routes = [
  {
    path: '',
    data: {
      title: 'payment'
    },
    children: [
      {
        path: '',
        redirectTo: 'payment'
      },
      {
        path: 'approved-bill-pay-status',
        component: ApprovedBillPayStatusComponent,
        data: {
          title: 'approved-bill-pay-status'
        }
      },
      {
        path: 'bill-payment',
        component: BillPaymentComponent,
        data: {
          title: 'bill-payment'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PaymentRoutingModule {}
