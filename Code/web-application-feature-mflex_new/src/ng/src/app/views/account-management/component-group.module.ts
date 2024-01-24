import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';

// Account Management Component
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { BarUnbarAccountComponent } from './bar-unbar-account.component';
import { CheckBalanceComponent } from './check-balance.component';
import { BillPaymentApprovalComponent } from "./billpayment-approval.component";
import { BillDataValidatorComponent } from "./billdata-validator.component";
import { CreateAccountComponent } from './create-account.component';
import { NgSelectModule } from '@ng-select/ng-select';

// Buttons Routing
import { RoutingModule } from './routing.module';
import {ValidatorModule} from "../../directives/validator.module";

@NgModule({
    imports: [
        CommonModule,
        RoutingModule,
        BsDropdownModule.forRoot(),
        FormsModule,
        NgSelectModule,
        ValidatorModule
    ],
    declarations: [
        BarUnbarAccountComponent,
        CheckBalanceComponent,
        BillPaymentApprovalComponent,
        BillDataValidatorComponent,
        CreateAccountComponent
    ]
})
export class AccountManagementModule { }
