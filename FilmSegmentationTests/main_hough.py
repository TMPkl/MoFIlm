import cv2 as cv
import numpy as np

from rect_seg_hough import detect_film_frame_hough, extract_and_perspective_correct

# Wczytaj obraz
img = cv.imread("a.jpg")

# Konwersja do skali szarości i negacja
img = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
img = ~img 

# Wykryj klatkę używając transformaty Hougha
detected, corners = detect_film_frame_hough(img, show_steps=True)

# Wyświetl wynik detekcji
cv.namedWindow('Detected', cv.WINDOW_NORMAL)
cv.resizeWindow('Detected', 800, 600)
cv.imshow("Detected", detected)

cv.waitKey(0)
cv.destroyAllWindows()

# Jeśli znaleziono klatkę, skoryguj perspektywę
if corners is not None:
    # Odwróć obraz z powrotem dla korekcji
    original_img = ~img
    corrected = extract_and_perspective_correct(original_img, corners)
    
    if corrected is not None:
        cv.namedWindow('Corrected', cv.WINDOW_NORMAL)
        cv.resizeWindow('Corrected', 1200, 800)
        cv.imshow("Corrected", corrected)
        cv.waitKey(0)
        cv.destroyAllWindows()
        
        # Zapisz wyniki
        cv.imwrite("detected_frame.jpg", detected)
        cv.imwrite("corrected_frame.jpg", corrected)
        print("✓ Zapisano wyniki jako 'detected_frame.jpg' i 'corrected_frame.jpg'")
