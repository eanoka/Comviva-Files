import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { CreatePaymentRequestComponent } from './create.component';
import { CheckBillStatusComponent } from './check-bill-status.component';
import { RequestStatusComponent } from './request-status.component';
import { RefreshBillsComponent } from './refresh-bills.component';
import { BillCollectionRequestComponent } from './bill-collection-request.component';

const routes: Routes = [
  {
    path: '',
    data: {
      title: 'payment-request'
    },
    children: [
      {
        path: 'refresh-bills',
        component: RefreshBillsComponent,
        data: {
          title: 'refresh-bills'
        }
      },
      {
        path: 'create',
        component: CreatePaymentRequestComponent,
        data: {
          title: 'Create Payment Request'
        }
      },
      {
        path: 'check-bill-status',
        component: CheckBillStatusComponent,
        data: {
          title: 'check-bill-status'
        }
      },
      {
        path: 'bill-collection-request',
        component: BillCollectionRequestComponent,
        data: {
          title: 'bill-collection-request'
        }
      },
      {
        path: 'request-status',
        component: RequestStatusComponent,
        data: {
          title: 'request-status'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PaymentRequestRoutingModule {}
