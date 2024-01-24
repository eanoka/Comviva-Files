import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgModule} from '@angular/core';

// Dropdowns Component
import {BsDropdownModule} from 'ngx-bootstrap/dropdown';
import {ApprovalStatusComponent} from './approval-status.component';
import {ApproveForPaymentComponent} from './approve-for-payment.component';

// Buttons Routing
import {PaymentApprovalRoutingModule} from './payment-approval-routing.module';
import {ModalModule} from "ngx-bootstrap/modal";
import {PaginatorModule} from "../../generic/components/paginator.module";
import {PerfectScrollbarModule} from "ngx-perfect-scrollbar";
import {RequestPdfDownloaderModule} from "../payment-request/request-pdf-generator.module";
import {BillTableModule} from "../manage-bill-data/bill-table.module";
import { PRApprovalHistoryModule } from '../payment-request/payment-request-approval-history.module';

// Angular
@NgModule({
    imports: [
        CommonModule,
        PaymentApprovalRoutingModule,
        BsDropdownModule.forRoot(),
        FormsModule,
        ModalModule,
        PaginatorModule,
        RequestPdfDownloaderModule,
        PerfectScrollbarModule,
        BillTableModule,
        PRApprovalHistoryModule
    ],
    declarations: [
        ApprovalStatusComponent,
        ApproveForPaymentComponent
    ]
})
export class PaymentApprovalModule {
}