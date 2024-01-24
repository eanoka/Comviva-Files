import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';

// Dropdowns Component
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { ApprovedBillPayStatusComponent } from './approved-bill-pay-status.component';
import { BillPaymentComponent } from './bill-payment.component';

// Buttons Routing
import { PaymentRoutingModule } from './payment-routing.module';
import {ModalModule} from "ngx-bootstrap/modal";
import {PaginatorModule} from "../../generic/components/paginator.module";
import {BillTableModule} from "../manage-bill-data/bill-table.module";
import {RequestPdfDownloaderModule} from "../payment-request/request-pdf-generator.module";
import { PRApprovalHistoryModule } from '../payment-request/payment-request-approval-history.module';

// Angular

@NgModule({
    imports: [
        CommonModule,
        PaymentRoutingModule,
        BsDropdownModule.forRoot(),
        FormsModule,
        ModalModule,
        PaginatorModule,
        BillTableModule,
        RequestPdfDownloaderModule,
        PRApprovalHistoryModule
    ],
  declarations: [
	ApprovedBillPayStatusComponent,
	BillPaymentComponent
  ]
})
export class PaymentModule { }