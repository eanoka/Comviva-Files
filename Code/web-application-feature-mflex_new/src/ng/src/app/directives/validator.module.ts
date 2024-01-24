import {NgModule} from '@angular/core';

// Buttons Routing
import {FormInputAsyncValidator} from "./async-validator.directive";
import {FormInputValidator} from "./validator.directive";

// Angular
@NgModule({
    declarations: [
        FormInputAsyncValidator,
        FormInputValidator
    ],
    exports: [
        FormInputAsyncValidator,
        FormInputValidator
    ]
})
export class ValidatorModule {
}