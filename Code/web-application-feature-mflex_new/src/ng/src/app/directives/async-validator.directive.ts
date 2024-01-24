import {
    Directive,
    Input
} from '@angular/core';
import {AbstractControl, AsyncValidator, NG_ASYNC_VALIDATORS, ValidationErrors} from "@angular/forms";
import {Observable} from "rxjs";

type Validator = {
    [key: string]: (value: any) => Observable<any>;
};

class Iterator {
    constructor(public next: Function) {
    }
}

class Synchronizer {
    private queue: any[] = []
    private lastExecute: number = 0
    private busy: boolean
    private timer: any
    private iterator: Iterator = new Iterator(() => {
        if(this.queue.length) {
            this.busy = true
            this.queue.shift()(this.iterator)
        } else {
            this.busy = false
        }
    })

    constructor(private ignoreWhileBusy: boolean, private threshold: number) {
    }

    execute(executor: Function) {
        if(!this.ignoreWhileBusy) {
            this.queue.push(executor)
            if(!this.busy) {
                this.iterator.next()
            }
        } else {
            let nowExecute = new Date().getTime()
            if(nowExecute - this.lastExecute >= this.threshold) {
                if(!this.busy) {
                    this.busy = true
                    executor(this.iterator)
                } else {
                    this.queue[0] = executor
                }
            } else {
                clearTimeout(this.timer)
                this.timer = setTimeout(() => {
                    if(!this.busy) {
                        this.busy = true
                        executor(this.iterator)
                    } else {
                        this.queue[0] = executor
                    }
                }, this.threshold)
            }
            this.lastExecute = nowExecute
        }
    }
}

@Directive({
    selector: '[validate-async][ngModel],[validate-async][FormControl]',
    providers: [
        {provide: NG_ASYNC_VALIDATORS, useExisting: FormInputAsyncValidator, multi: true}
    ]
})
export class FormInputAsyncValidator implements AsyncValidator {
    @Input("validate-async") validators: Validator
    public synchronizer: Synchronizer = new Synchronizer(true, 800)

    validate(control: AbstractControl): Observable<ValidationErrors | null> {
        return new Observable<ValidationErrors | null>(observer => {
            this.synchronizer.execute((iterator) => {
                let toEmit: ValidationErrors = {}
                let validatorCount = 0
                for(var validator in this.validators) {
                    let validatorName = validator
                    let validatorFn = this.validators[validator]
                    validatorCount++
                    validatorFn(control.value).subscribe(x => {
                        if(x != null) {
                            toEmit[validatorName] = x
                        }
                        validatorCount--
                        if(validatorCount == 0) {
                            observer.next(toEmit)
                            observer.complete()
                            iterator.next()
                        }
                    }, e => {
                        validatorCount--
                        if(validatorCount == 0) {
                            observer.next(toEmit)
                            observer.complete()
                            iterator.next()
                        }
                    })
                }
            })
        });
    }
}