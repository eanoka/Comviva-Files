import {NgModule} from '@angular/core';
import {CommonModule} from "@angular/common";
import {RequestPdfDownloaderComponent} from "./request-pdf-generator.component"

// Angular
@NgModule({
    imports: [
        CommonModule,
    ],
    declarations: [
        RequestPdfDownloaderComponent
    ],
    exports: [
        RequestPdfDownloaderComponent
    ]
})
export class RequestPdfDownloaderModule {
}