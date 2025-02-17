package co.eivo.brother_printer;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

/** BrotherPrinterPlugin */
public class BrotherPrinterPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Context context;
  private Activity activity;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "brother_printer");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
  }

  @Override
  public void onDetachedFromActivity() {
  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
  }

  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("searchDevices")) {
      searchDevices(call, result);
    }
    else if (call.method.equals("printPDF")) {
      printPDF(call, result);
    }
    else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  public void searchDevices(@NonNull MethodCall call, @NonNull final Result result) {
    int delay = call.argument("delay");
    ArrayList<String> printerNames = call.argument("printerNames");

    PrinterDiscovery.getInstance().start(delay, printerNames, new BRPrinterDiscoveryCompletion(){
      public void completion(final ArrayList<Map<String, String>> devices, final Exception exception) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            if (exception != null) {
              if (exception instanceof PrinterErrorException) {
                PrinterErrorException castException = (PrinterErrorException)exception;
                result.error(castException.code.toString(), castException.getMessage(), null);
              }
              else {
                result.error("unknown", exception.getMessage(), null);
              }
            } else {
              result.success(devices);
            }
          }
        });
      }
    });
  }

  public void printPDF(@NonNull MethodCall call, @NonNull final Result result) {
    String path = call.argument("path");
    int copies = call.argument("copies");
    int modelCode = call.argument("modelCode");
    String ipAddress = call.argument("ipAddress");
    String macAddress = call.argument("macAddress");
    String bleAdvertiseLocalName = call.argument("bleAdvertiseLocalName");
    String paperSettingsPath = call.argument("paperSettingsPath");
    String labelSize = call.argument("labelSize");

    PrinterSession session = new PrinterSession();
    session.print(activity, context, modelCode, path, copies, ipAddress, macAddress, bleAdvertiseLocalName, paperSettingsPath, labelSize, new BRPrinterSessionCompletion(){
      public void completion(final Exception exception) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            if (exception != null) {
              if (exception instanceof PrinterErrorException) {
                PrinterErrorException castException = (PrinterErrorException)exception;
                result.error(castException.code.toString(), castException.getMessage(), null);
              }
              else {
                result.error(exception.getMessage(), exception.getMessage(), null);
              }
            } else {
              result.success(null);
            }
          }
        });
      }
    });
  }
}
