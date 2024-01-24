import {NgModule} from '@angular/core';
import {PaginatorComponent} from "./paginator.component";
import {CommonModule} from "@angular/common";
import {BsDropdownModule} from "ngx-bootstrap/dropdown";

// Angular
@NgModule({
    imports: [
        CommonModule,
        BsDropdownModule
    ],
    declarations: [
        PaginatorComponent
    ],
    exports: [
        PaginatorComponent
    ]
})
export class PaginatorModule {
}