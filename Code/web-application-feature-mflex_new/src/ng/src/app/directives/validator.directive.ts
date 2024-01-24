import {
    Directive,
    Input
} from '@angular/core';
import {AbstractControl, NgModel, NG_VALIDATORS, ValidationErrors, Validator} from "@angular/forms";
import * as m from "moment";
import * as $ from "jquery";

type Rule = {
    [key: string]: any;
};

let Validators: any = {
    compare: function(aField: NgModel, config: any) {
        return this[config.op + "-" + config.type].call(Validators, aField, config);
    },
    either: function(aField: NgModel, bField: NgModel) {
        if(aField.value || bField.value) {
            return null;
        }
        return {either: "invalid"};
    },
    gte: function(aField: NgModel, config: any) {
        return this["gte-" + config.type].call(Validators, aField, config);
    },
    "gte-date": function(aField: NgModel, config: any) {
        if(aField.value && config.ref) {
            let value
            if(config.ref instanceof NgModel) {
                value = config.ref.value
            }
            if(value) {
                let thisDate = m(aField.value, "YYYY-MM-DD")
                let thatDate = m(value, "YYYY-MM-DD")
                if(thisDate.isBefore(thatDate)) {
                    let _return: any = {}
                    if(config.op) {
                        _return.compare = "invalid"
                    } else if(config.type) {
                        _return.gte = "invalid"
                    } else {
                        _return["gte-date"] = "invalid"
                    }
                    return _return
                }
            }
        }
        return null
    },
    maxdiff: function(aField: NgModel, config: any) {
        return this["maxdiff-" + config.type].call(Validators, aField, config);
    },
    "maxdiff-date": function(aField: NgModel, config: any) {
        if(aField.value && config.ref) {
            let value
            if(config.ref instanceof NgModel) {
                value = config.ref.value
            }
            if(!value) {
                value = m()
            }
            let thisDate = m(aField.value, "YYYY-MM-DD")
            let thatDate = m(value, "YYYY-MM-DD")
            if(thatDate.diff(thisDate) > config.diff) {
                let _return: any = {}
                if(config.op) {
                    _return.compare = "invalid"
                } else if(config.type) {
                    _return.maxdiff = "invalid"
                } else {
                    _return["maxdiff-date"] = "invalid"
                }
                return _return
            }
        }
        return null
    },
    trigger: function(aField: NgModel, bField: NgModel) {
        if(!Validators.trigger_stack) {
            Validators.trigger_stack = [aField, bField]
        } else if(Validators.trigger_stack.indexOf(bField) != -1) {
            return null
        } else {
            Validators.trigger_stack.push(bField)
        }
        bField.control.updateValueAndValidity()
        Validators.trigger_stack.pop()
        if(Validators.trigger_stack.length == 1) {
            delete Validators.trigger_stack
        }
        return null;
    }
}

@Directive({
    selector: '[validate][ngModel],[validate][NgModel]',
    providers: [
        {provide: NG_VALIDATORS, useExisting: FormInputValidator, multi: true}
    ]
})
export class FormInputValidator implements Validator {
    @Input("validate") rules: Rule

    validate(control: AbstractControl): ValidationErrors | null {
        let returnables: any = {}
        for(let name in this.rules) {
            if(Validators[name]) {
                 $.extend(returnables, Validators[name].call(Validators, control, this.rules[name]))
            }
        }
        if($.isEmptyObject(returnables)) {
            return null;
        }
        return returnables;
    }
}