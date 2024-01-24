import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgModule} from '@angular/core';

// Dropdowns Component
import {BsDropdownModule} from 'ngx-bootstrap/dropdown';
import {CreatePaymentRequestComponent} from './create.component';
import {CheckBillStatusComponent} from './check-bill-status.component';
import {RequestStatusComponent} from './request-status.component';
import {RefreshBillsComponent} from './refresh-bills.component';

// Buttons Routing
import {PaymentRequestRoutingModule} from './payment-request-routing.module';
import { NgSelectModule } from '@ng-select/ng-select';
import {PaginatorModule} from "../../generic/components/paginator.module";
import {ValidatorModule} from "../../directives/validator.module";
import {BillTableModule} from "../manage-bill-data/bill-table.module";
import {RequestPdfDownloaderModule} from "./request-pdf-generator.module";
import {ModalModule} from "ngx-bootstrap/modal"; 
import { BillCollectionRequestComponent } from './bill-collection-request.component';
import {CollapseModule} from 'ngx-bootstrap/collapse';
import { PRApprovalHistoryModule } from './payment-request-approval-history.module';

// Angular
@NgModule({
    imports: [
        CommonModule,
        PaymentRequestRoutingModule,
        BsDropdownModule.forRoot(),
        FormsModule,
        NgSelectModule,
        PaginatorModule,
        ValidatorModule,
        BillTableModule,
        RequestPdfDownloaderModule,
        ModalModule,
        CollapseModule,
        PRApprovalHistoryModule
        
    ],
    declarations: [
        CreatePaymentRequestComponent,
        CheckBillStatusComponent,
        RequestStatusComponent,
        RefreshBillsComponent,
        BillCollectionRequestComponent
    ]
})
export class PaymentRequestModule {
}
