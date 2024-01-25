import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OtpPwdComponent } from './otp-pwd.component';

describe('OtpPwdComponent', () => {
  let component: OtpPwdComponent;
  let fixture: ComponentFixture<OtpPwdComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [OtpPwdComponent]
    });
    fixture = TestBed.createComponent(OtpPwdComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
