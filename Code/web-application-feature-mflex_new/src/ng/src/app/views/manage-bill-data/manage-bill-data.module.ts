import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgModule} from '@angular/core';

// Manage Bill Data Component
import {CreateBillDataComponent} from './create-bill-data.component';
import {EditDataComponent} from './edit-data.component';
import {ValidateDataComponent} from './validate-bill-data.component';

// Buttons Routing
import {ManageBillDataRoutingModule} from './manage-bill-data-routing.module';
import { NgSelectModule } from '@ng-select/ng-select';
import {AppFileUpload} from "../../directives/fileupload.directive";
import {BsDropdownModule} from "ngx-bootstrap/dropdown";
import {PaginatorModule} from "../../generic/components/paginator.module";

// Angular
@NgModule({
    imports: [
        CommonModule,
        ManageBillDataRoutingModule,
        FormsModule,
        NgSelectModule,
        BsDropdownModule.forRoot(),
        PaginatorModule
    ],
    declarations: [
        CreateBillDataComponent,
        EditDataComponent,
        ValidateDataComponent,
        AppFileUpload
    ]
})
export class ManageBillDataModule {
}