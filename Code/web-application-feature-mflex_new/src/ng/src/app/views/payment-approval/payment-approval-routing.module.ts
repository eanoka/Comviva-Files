import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { ApprovalStatusComponent } from './approval-status.component';
import { ApproveForPaymentComponent } from './approve-for-payment.component';

const routes: Routes = [
  {
    path: '',
    data: {
      title: 'payment-approval'
    },
    children: [
      {
        path: '',
        redirectTo: 'payment-approval'
      },
      {
        path: 'approval-status',
        component: ApprovalStatusComponent,
        data: {
          title: 'approval-status'
        }
      },
      {
        path: 'approve-for-payment',
        component: ApproveForPaymentComponent,
        data: {
          title: 'approve-for-payment'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PaymentApprovalRoutingModule {}
