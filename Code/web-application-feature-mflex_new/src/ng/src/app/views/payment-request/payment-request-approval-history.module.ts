import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ModalModule} from "ngx-bootstrap/modal"; 
import { PRApprovalHistoryComponent } from './payment-request-approval-history.component';

// Angular
@NgModule({
    imports: [
        CommonModule,
        ModalModule
    ],
    declarations: [
        PRApprovalHistoryComponent
    ],
    exports: [
        PRApprovalHistoryComponent
    ]
})
export class PRApprovalHistoryModule {
}