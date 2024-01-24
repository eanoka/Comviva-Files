import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';

// Transaction Reports Component
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { DetailTransactionReportComponent } from './detail-transaction-report.component';
import { PrepaidTokenEnquiryComponent } from './prepaid-token-enquiry.component';

// Buttons Routing
import { ButtonsRoutingModule } from './buttons-routing.module';
import {ValidatorModule} from "../../directives/validator.module";
import {PaginatorModule} from "../../generic/components/paginator.module";
import { VatReportComponent } from './vat-report.component';

// Angular

@NgModule({
    imports: [
        CommonModule,
        ButtonsRoutingModule,
        BsDropdownModule.forRoot(),
        FormsModule,
        ValidatorModule,
        PaginatorModule
    ],
  declarations: [
	DetailTransactionReportComponent,
	PrepaidTokenEnquiryComponent,
	VatReportComponent
  ]
})
export class TransactionReportsModule { }
