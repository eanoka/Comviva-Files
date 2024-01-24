import {NgModule} from '@angular/core';
import {CommonModule} from "@angular/common";
import {BsDropdownModule} from "ngx-bootstrap/dropdown";
import {BillTableComponent} from "./bill-table.component";
import {PaginatorModule} from "../../generic/components/paginator.module";
import {RequestPdfDownloaderModule} from "../payment-request/request-pdf-generator.module";

// Angular
@NgModule({
    imports: [
        CommonModule,
        BsDropdownModule,
        PaginatorModule,
        RequestPdfDownloaderModule
    ],
    declarations: [
        BillTableComponent
    ],
    exports: [
        BillTableComponent
    ]
})
export class BillTableModule {
}