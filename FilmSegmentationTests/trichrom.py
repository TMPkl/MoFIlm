import cv2
import numpy as np

# wczytanie obrazów w skali szarości
r = cv2.imread("red.jpg", cv2.IMREAD_GRAYSCALE)
g = cv2.imread("green.jpg", cv2.IMREAD_GRAYSCALE)
b = cv2.imread("blue.jpg", cv2.IMREAD_GRAYSCALE)


shape = r.shape
g.resize(shape)
b.resize(shape)

# sprawdzenie czy obrazy mają ten sam rozmiar
if r.shape != g.shape or r.shape != b.shape:
    raise ValueError("Obrazy muszą mieć ten sam rozmiar!")

# połączenie kanałów w RGB
rgb = cv2.merge([b, g, r])  # OpenCV używa BGR, nie RGB

# zapis wyniku
cv2.imwrite("output_rgb.png", rgb)

# (opcjonalnie) podgląd
cv2.imshow("RGB image", rgb)
cv2.waitKey(0)
cv2.destroyAllWindows()