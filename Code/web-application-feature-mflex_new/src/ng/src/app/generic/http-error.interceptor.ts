import {
    HttpEvent,
    HttpInterceptor,
    HttpHandler,
    HttpRequest,
    HttpErrorResponse
} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import { catchError } from 'rxjs/operators';
import {Snackbar} from "../views/blocks/snackbar.component";

export class HttpErrorInterceptor implements HttpInterceptor {
    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next.handle(request)
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    if(error.error && error.error.message) {
                        Snackbar.show("danger", error.error.message);
                    } else {
                        Snackbar.show("danger", "Error from server: Unrecognized")
                    }
                    return throwError(error);
                })
            )
    }
}