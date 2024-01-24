import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { DetailTransactionReportComponent } from './detail-transaction-report.component';
import { PrepaidTokenEnquiryComponent } from './prepaid-token-enquiry.component';
import { VatReportComponent } from './vat-report.component';

const routes: Routes = [
  {
    path: '',
    data: {
      title: 'transaction-reports'
    },
    children: [
      {
        path: '',
        redirectTo: 'transaction-reports'
      },
      {
        path: 'detail-transaction-report',
        component: DetailTransactionReportComponent,
        data: {
          title: 'detail-transaction-report'
        }
      },
      {
        path: 'prepaid-token-enquiry',
        component: PrepaidTokenEnquiryComponent,
        data: {
          title: 'prepaid-token-enquiry'
        }
      },
      {
        path: 'vat-report',
        component: VatReportComponent,
        data: {
          title: 'vat-report'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ButtonsRoutingModule {}
